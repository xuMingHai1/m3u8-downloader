package xyz.xuminghai.m3u8_downloader.http;

import lombok.extern.slf4j.Slf4j;
import xyz.xuminghai.m3u8_downloader.config.CommonData;
import xyz.xuminghai.m3u8_downloader.m3u8.KeyMethodEnum;
import xyz.xuminghai.m3u8_downloader.m3u8.M3U8Key;
import xyz.xuminghai.m3u8_downloader.m3u8.MediaPlay;
import xyz.xuminghai.m3u8_downloader.m3u8.TsFile;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

/**
 * 2024/4/25 下午2:03 星期四<br/>
 *
 * @author xuMingHai
 */
@Slf4j
public class M3U8HttpClient implements AutoCloseable {

    /**
     * 最大等待时长阈值
     */
    static final Duration MAX_TIMEOUT = Duration.ofSeconds(60L);

    /**
     * 默认休眠时长
     */
    private static final long SLEEP_TIME = 10L;

    private final ConcurrentLinkedQueue<Thread> sleepQueue = new ConcurrentLinkedQueue<>();
    private final LongAdder networkExceptionCount = new LongAdder();
    private static final long NETWORK_EXCEPTION_BASE = 20L;
    private final AtomicLong networkExceptionThreshold = new AtomicLong();
    private final LongAdder downloadByteCount = new LongAdder();

    private final Duration timeout;
    private final Path tempDirPath;

    private final HttpClient httpClient;


    public M3U8HttpClient(Duration timeout, Path tempDirPath, ProxySelector proxySelector) {
        this.timeout = timeout;
        this.httpClient = HttpClient.newBuilder()
                .executor(CommonData.EXECUTOR)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .proxy(proxySelector)
                .connectTimeout(timeout)
                .build();
        this.tempDirPath = tempDirPath;
    }


    public Path downloadM3U8(URI uri) throws InterruptedException, IOException {
        networkExceptionThreshold.set(NETWORK_EXCEPTION_BASE);
        final Semaphore semaphore = new Semaphore(1);
        final DownloadHttpRequest downloadHttpRequest = headRequest(uri, semaphore);
        return download(tempDirPath.resolve("index.m3u8"),
                downloadHttpRequest, semaphore);
    }

    public byte[] downloadKey(URI uri) throws IOException, InterruptedException {
        log.info("开始下载密钥");
        networkExceptionThreshold.set(NETWORK_EXCEPTION_BASE);
        final Semaphore semaphore = new Semaphore(1);
        final DownloadHttpRequest downloadHttpRequest = headRequest(uri, semaphore);
        final Path aesFilePath = download(tempDirPath.resolve(Instant.now().toEpochMilli() + "-aes-128.key"),
                downloadHttpRequest, semaphore);
        log.info("密钥下载成功，aesFilePath = {}", aesFilePath);
        return Files.readAllBytes(aesFilePath);
    }

    public List<Future<TsFile>> downloadTs(List<MediaPlay> playList) {
        log.info("创建ts文件列表下载任务");
        final int playListSize = playList.size();
        final List<Future<TsFile>> tsTaskList = new ArrayList<>(playListSize);
        networkExceptionThreshold.set(playListSize);
        final Semaphore semaphore = new Semaphore(playListSize);
        for (MediaPlay mediaPlay : playList) {
            // 提交执行ts下载任务
            tsTaskList.add(CommonData.EXECUTOR.submit(newTsTask(mediaPlay, semaphore)));
        }
        return tsTaskList;
    }

    public long currentDownloadByte() {
        return downloadByteCount.sum();
    }

    public void reset() {
        networkExceptionCount.reset();
        networkExceptionThreshold.set(0L);
        downloadByteCount.reset();
    }

    private Callable<TsFile> newTsTask(MediaPlay mediaPlay, Semaphore semaphore) {
        return () -> {
            log.debug("开始下载ts文件");
            final DownloadHttpRequest downloadHttpRequest = headRequest(mediaPlay.uri(), semaphore);
            final Path tsFilePath = download(tempDirPath.resolve(mediaPlay.sequence() + ".ts"),
                    downloadHttpRequest, semaphore);
            log.debug("ts文件下载成功，tsFilePath = {}", tsFilePath);
            // 解密文件
            decryptContent(mediaPlay.key(), tsFilePath);
            return new TsFile(mediaPlay.sequence(), tsFilePath);
        };
    }


    private void decryptContent(M3U8Key m3u8Key, Path tsFilePath) throws IOException, InvalidAlgorithmParameterException,
            InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException {
        if (m3u8Key.getMethod() == KeyMethodEnum.AES_128) {
            log.debug("解密AES-128加密数据");
            final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            // 初始化解密
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(m3u8Key.getKey(), "AES"),
                    new IvParameterSpec(m3u8Key.getIv()));
            final Path decryptFilePath = tsFilePath.getParent().resolve(tsFilePath.getFileName().toString().concat(".decrypt"));
            // 解密为新文件
            try (CipherInputStream inputStream = new CipherInputStream(Files.newInputStream(tsFilePath), cipher)) {
                Files.copy(inputStream, decryptFilePath);
            }
            // 删除未解密文件
            Files.delete(tsFilePath);
            // 重命名新文件
            Files.move(decryptFilePath, tsFilePath);
            log.debug("解密AES-128数据成功");
        }
    }

    private DownloadHttpRequest headRequest(URI uri, Semaphore semaphore) throws InterruptedException, IOException {
        final HeadHttpRequest headHttpRequest = new HeadHttpRequest(sleepQueue, uri, timeout);
        for (; ; ) {
            // http2 并发流量控制问题
            semaphore.acquire();
            try {
                final HttpResponse<Void> response = logSend(headHttpRequest.newHttpRequest(), HttpResponse.BodyHandlers.discarding());
                final HttpHeaders headers = response.headers();
                final long contentLength = headers.firstValue("content-length").map(Long::parseLong).orElse(Long.MAX_VALUE);
                final String acceptRanges = headers.firstValue("accept-ranges").orElse(null);
                return new DownloadHttpRequest(sleepQueue, uri, timeout,
                        response.version(), contentLength,
                        "bytes".equals(acceptRanges));
            }
            catch (IOException e) {
                httpIOExceptionHandler(e, headHttpRequest, semaphore);
            }
            finally {
                semaphore.release();
            }
        }
    }


    private Path download(Path filePath, DownloadHttpRequest downloadHttpRequest, Semaphore semaphore) throws InterruptedException, IOException {
        if (Files.notExists(filePath)) {
            log.debug("创建文件，filePath = {}", filePath);
            Files.createFile(filePath);
        }
        for (; ; ) {
            if (downloadHttpRequest.version() == HttpClient.Version.HTTP_2) {
                semaphore.acquire();
            }
            // 是否已经下载完成
            final long fileSize = Files.size(filePath);
            if (downloadHttpRequest.complete(fileSize)) {
                return filePath;
            }
            final HttpRequest httpRequest = downloadHttpRequest.newHttpRequest(fileSize);
            try {
                // 发送http请求
                final HttpResponse<Path> httpResponse = logSend(httpRequest,
                        _ -> new DownloadPathSubscriber(filePath, downloadHttpRequest.openOptions(), downloadByteCount)
                );
                final HttpHeaders headers = httpResponse.headers();
                final ContentEncoding contentEncoding = ContentEncoding.of(headers.firstValue("content-encoding").orElse(null));
                // 解压缩文件
                unzipContent(contentEncoding, filePath);
                return filePath;
            }
            catch (IOException e) {
                httpIOExceptionHandler(e, downloadHttpRequest, semaphore);
            }
            finally {
                if (downloadHttpRequest.version() == HttpClient.Version.HTTP_2) {
                    semaphore.release();
                }
            }
        }

    }

    /**
     * 是否需要解压缩内存
     *
     * @param contentEncoding content-encoding格式
     * @param tsFilePath      ts文件路径
     * @throws IOException 文件异常
     */
    private void unzipContent(ContentEncoding contentEncoding, Path tsFilePath) throws IOException {
        if (ContentEncoding.IDENTITY != contentEncoding) {
            log.atDebug().setMessage("""
                            解压缩文件
                            contentEncoding = {}
                            zipFileSize = {}
                            """)
                    .addArgument(contentEncoding)
                    .addArgument(() -> {
                        try {
                            return Files.size(tsFilePath);
                        }
                        catch (IOException _) {
                            // 不处理
                        }
                        return 0L;
                    }).log();
            final Path unzipFilePath = tsFilePath.getParent().resolve(contentEncoding + "-" + tsFilePath.getFileName()
                    .toString().concat(".unzip"));
            // 解压为新的文件
            try (InputStream inputStream = unzipInputStream(contentEncoding, Files.newInputStream(tsFilePath))) {
                Files.copy(inputStream, unzipFilePath);
            }
            // 删除未解压文件
            Files.delete(tsFilePath);
            // 重命名新文件
            Files.move(unzipFilePath, tsFilePath);
            log.atDebug().setMessage("""
                            解压缩文件成功
                            unzipFileSize = {}
                            """)
                    .addArgument(() -> {
                        try {
                            return Files.size(tsFilePath);
                        }
                        catch (IOException _) {
                            // 不处理
                        }
                        return 0L;
                    }).log();
        }
    }

    private InputStream unzipInputStream(ContentEncoding contentEncoding, InputStream inputStream) throws IOException {
        return switch (contentEncoding) {
            case GZIP -> new GZIPInputStream(inputStream);
            case DEFLATE -> new InflaterInputStream(inputStream);
            case null, default -> inputStream;
        };
    }

    private void networkException() throws ConnectException {
        networkExceptionCount.increment();
        // 超过指定阈值抛出网络异常
        if (networkExceptionCount.sum() >= networkExceptionThreshold.get()) {
            throw new ConnectException("网络连接异常，请检查网络连接或代理服务器");
        }
    }

    private void httpIOExceptionHandler(IOException e, HttpRequestExpand httpRequestExpand, Semaphore semaphore) throws IOException, InterruptedException {
        switch (e) {
            case HttpStatusCodeException _ -> log.trace("http 状态码异常", e);
            case HttpConnectTimeoutException _ -> {
                log.trace("http连接超时", e);
                networkException();
            }
            case HttpTimeoutException _ -> {
                log.trace("http超时", e);
                // 增加http响应时长
                httpRequestExpand.plusTimeout(timeout);
            }
            case HttpStatusCode4xxException httpStatusCode4xxException -> {
                if (httpStatusCode4xxException.getHttpResponse().statusCode() == 403) {
                    throw new HttpStatusCode4xxException("服务器拒绝请求，请尝试使用代理服务器",
                            httpStatusCode4xxException.getHttpResponse(),
                            httpStatusCode4xxException);
                }
                throw httpStatusCode4xxException;
            }
            case ConnectException _ -> {
                log.trace("http连接异常", e);
                networkException();
            }
            case SSLHandshakeException _ -> {
                log.trace("SSL握手异常", e);
                networkException();
            }
            default -> {
                final String message = Optional.ofNullable(e.getMessage()).orElse("");
                // http2 并发流量控制问题的
                if ("too many concurrent streams".equals(message)) {
                    log.trace("http2 并发流量控制", e);
                    if (httpRequestExpand instanceof DownloadHttpRequest downloadHttpRequest) {
                        if (downloadHttpRequest.version() == HttpClient.Version.HTTP_2) {
                            semaphore.acquire();
                        }
                    }
                }
                else if ("Connection reset".equals(message)) {
                    log.trace("连接重置", e);
                    httpRequestExpand.plusSleep(SLEEP_TIME);
                    networkException();
                }
                else if (message.endsWith(": GOAWAY received")) {
                    log.trace("收到 http2 GOAWAY 帧", e);
                    httpRequestExpand.plusSleep(SLEEP_TIME);
                }
                // 可能是服务器异常、网络异常
                else if (message.startsWith("fixed content-length: ")) {
                    log.trace("http响应体内容缺失", e);
                    httpRequestExpand.plusSleep(SLEEP_TIME);
                }
                else if (message.startsWith("BUFFER_UNDERFLOW with EOF,")) {
                    log.trace(e.getMessage(), e);
                    httpRequestExpand.plusSleep(SLEEP_TIME);
                }
                // HTTP2存在的问题，似乎是一个bug
                else if ("EOF reached while reading".equals(message)) {
                    log.trace("http2 EOF异常", e);
                    httpRequestExpand.plusSleep(SLEEP_TIME);
                }
                else if ("Received RST_STREAM: Stream not processed".equals(message)) {
                    log.trace("服务器未处理流", e);
                    httpRequestExpand.plusSleep(SLEEP_TIME);
                }
                else if ("Received RST_STREAM: Internal error".equals(message)) {
                    log.trace("服务器内部错误", e);
                    httpRequestExpand.plusSleep(SLEEP_TIME);
                }
                else if ("HTTP/1.1 header parser received no bytes".equals(message)) {
                    log.trace("HTTP/1.1 header 没有数据", e);
                    httpRequestExpand.plusSleep(SLEEP_TIME);
                }
                else {
                    throw e;
                }
            }
        }

    }


    private <T> HttpResponse<T> logSend(HttpRequest httpRequest, HttpResponse.BodyHandler<T> bodyHandler) throws IOException, InterruptedException {
        log.atTrace().setMessage("""
                        HttpRequest
                        {}
                        """)
                .addArgument(() -> httpRequestLog(httpRequest))
                .log();
        // 发送http请求
        final HttpResponse<T> httpResponse = httpClient.send(httpRequest, responseInfo -> {
            // 接收响应体前处理
            return switch (responseInfo.statusCode()) {
                // 接收响应体
                case 200, 206 -> bodyHandler.apply(responseInfo);
                // 丢弃响应体
                default -> HttpResponse.BodySubscribers.replacing(null);
            };
        });
        log.atTrace().setMessage("""
                        HttpResponse
                        {}
                        """)
                .addArgument(() -> httpResponseLog(httpResponse))
                .log();
        // 响应状态码处理
        final int statusCode = httpResponse.statusCode();
        if (statusCode == 200 || statusCode == 206) {
            final Thread thread = sleepQueue.poll();
            // 中断等待队列中的线程
            if (thread != null) {
                thread.interrupt();
            }
            return httpResponse;
        }
        // 客户端异常，可能需要结束任务
        else if (statusCode >= 400 && statusCode < 500) {
            throw new HttpStatusCode4xxException("""
                    HttpRequest
                    %s
                    HttpResponse
                    %s
                    """.formatted(httpRequestLog(httpRequest), httpResponseLog(httpResponse)),
                    httpResponse);
        }
        else {
            throw new HttpStatusCodeException("""
                    HttpRequest
                    %s
                    HttpResponse
                    %s
                    """.formatted(httpRequestLog(httpRequest), httpResponseLog(httpResponse)));
        }
    }

    private String httpRequestLog(HttpRequest httpRequest) {
        return """
                method = %s
                uri = %s
                timeout = %s
                version = %s
                headers = %s
                """.formatted(httpRequest.method(),
                httpRequest.uri(),
                httpRequest.timeout(),
                httpRequest.version(),
                httpRequest.headers());
    }

    private String httpResponseLog(HttpResponse<?> httpResponse) {
        return """
                statusCode = %d
                uri = %s
                version = %s
                headers = %s
                """.formatted(httpResponse.statusCode(),
                httpResponse.uri(),
                httpResponse.version(),
                httpResponse.headers());
    }

    @Override
    public void close() {
        httpClient.shutdownNow();
        sleepQueue.clear();
    }
}
