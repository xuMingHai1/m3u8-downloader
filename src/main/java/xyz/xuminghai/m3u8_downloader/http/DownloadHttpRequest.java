package xyz.xuminghai.m3u8_downloader.http;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 2024/5/23 下午4:56 星期四<br/>
 *
 * @author xuMingHai
 */
final class DownloadHttpRequest extends HttpRequestExpand {

    /**
     * http版本
     */
    private final HttpClient.Version version;

    /**
     * 内容长度
     */
    private final long contentLength;

    /**
     * 是否支持范围请求
     */
    private final boolean range;

    public DownloadHttpRequest(ConcurrentLinkedQueue<Thread> sleepQueue, URI uri, Duration timeout,
                               HttpClient.Version version, long contentLength,
                               boolean range) {
        super(sleepQueue, uri, timeout);
        this.version = version;
        this.contentLength = contentLength;
        this.range = range;
    }


    public HttpRequest newHttpRequest(long length) throws InterruptedException {
        sleep();
        final HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .version(version)
                .timeout(timeout)
                .header("accept-encoding", ContentEncoding.CONTENT_ENCODING);

        // 支持范围请求
        if (range) {
            builder.header("range", "bytes=" + length + "-");
        }
        return builder.GET().build();
    }

    public OpenOption[] openOptions() {
        if (range) {
            return new OpenOption[]{StandardOpenOption.WRITE,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND,
                    StandardOpenOption.DSYNC};
        }
        return new OpenOption[]{StandardOpenOption.WRITE,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.DSYNC};
    }

    public boolean complete(long fileSize) {
        return contentLength == fileSize;
    }

    public HttpClient.Version version() {
        return version;
    }

}
