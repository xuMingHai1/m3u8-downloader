package xyz.xuminghai.m3u8_downloader.http;

import java.io.IOException;

/**
 * 2024/4/27 上午2:10 星期六<br/>
 * 客户端状态码异常
 *
 * @author xuMingHai
 */
public class HttpStatusCode4xxException extends IOException {

    public HttpStatusCode4xxException(String message) {
        super(message);
    }
}
