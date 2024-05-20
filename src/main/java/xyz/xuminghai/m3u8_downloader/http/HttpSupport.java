package xyz.xuminghai.m3u8_downloader.http;

import lombok.Data;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.time.Duration;

/**
 * 2024/4/26 上午1:02 星期五<br/>
 * 针对http请求的一些操作
 *
 * @author xuMingHai
 */
@Data
public class HttpSupport {

    /**
     * http请求URI
     */
    private final URI uri;

    /**
     * http版本
     */
    private final HttpClient.Version version;

    /**
     * 超时时间
     */
    private Duration timeout;

    /**
     * 内容长度
     */
    private final long contentLength;

    /**
     * 内容编码
     */
    private final ContentEncoding contentEncoding;

    /**
     * 是否支持范围请求
     */
    private boolean range;

    /**
     * 请求次数统计
     */
    private int requestCount;

    public HttpSupport(URI uri, HttpClient.Version version, Duration timeout, long contentLength,
                       ContentEncoding contentEncoding, boolean range) {
        this.uri = uri;
        this.version = version;
        this.timeout = timeout;
        this.contentLength = contentLength;
        this.contentEncoding = contentEncoding;
        this.range = range;
    }

    public HttpRequest newHttpRequest(long length) {
        requestCount++;
        final HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .version(version)
                .timeout(timeout)
                .header("accept-encoding", "gzip, deflate");
        // 请求次数了超过阈值
        if (requestCount > 1) {
            if (M3U8HttpClient.MAX_TIMEOUT.compareTo(timeout) > 0) {
                // 响应时间翻倍等待，可能文件比较大，之后的在此基础上翻倍
                builder.timeout(timeout = timeout.plus(timeout));
            }
            // 放弃范围请求，可能有的服务器范围请求慢
            if (requestCount > 5) {
                range = false;
            }
        }
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

}
