package xyz.xuminghai.m3u8_downloader.http;

import lombok.Getter;

import java.io.IOException;
import java.net.http.HttpResponse;

/**
 * 2024/4/27 上午2:10 星期六<br/>
 * 客户端状态码异常
 *
 * @author xuMingHai
 */
@Getter
public class HttpStatusCode4xxException extends IOException {

    private final HttpResponse<?> httpResponse;

    public HttpStatusCode4xxException(String message, HttpResponse<?> httpResponse) {
        super(message);
        this.httpResponse = httpResponse;
    }

    public HttpStatusCode4xxException(String message, HttpResponse<?> httpResponse, Throwable cause) {
        super(message, cause);
        this.httpResponse = httpResponse;
    }
}
