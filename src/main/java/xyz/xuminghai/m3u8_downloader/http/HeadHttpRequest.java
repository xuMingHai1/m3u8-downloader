package xyz.xuminghai.m3u8_downloader.http;

import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 2024/5/23 下午4:10 星期四<br/>
 *
 * @author xuMingHai
 */
final class HeadHttpRequest extends HttpRequestExpand {


    public HeadHttpRequest(ConcurrentLinkedQueue<Thread> sleepQueue, URI uri, Duration timeout) {
        super(sleepQueue, uri, timeout);
    }

    public HttpRequest newHttpRequest() throws InterruptedException {
        sleep();
        return HttpRequest.newBuilder(uri)
                .timeout(timeout)
                .HEAD().build();
    }


}
