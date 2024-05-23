package xyz.xuminghai.m3u8_downloader.http;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 2024/5/23 下午3:30 星期四<br/>
 *
 * @author xuMingHai
 */
sealed abstract class HttpRequestExpand
        permits HeadHttpRequest, DownloadHttpRequest {

    private final ConcurrentLinkedQueue<Thread> sleepQueue;
    private long sleep;

    protected final URI uri;

    protected Duration timeout;

    public HttpRequestExpand(ConcurrentLinkedQueue<Thread> sleepQueue, URI uri, Duration timeout) {
        this.sleepQueue = sleepQueue;
        this.uri = uri;
        this.timeout = timeout;
    }

    public void plusTimeout(Duration duration) {
        // 没有超过最大等待时间
        if (M3U8HttpClient.MAX_TIMEOUT.compareTo(timeout) > 0) {
            timeout = timeout.plus(duration);
        }
    }

    public void plusSleep(long millisecond) {
        sleep += millisecond;
    }

    protected void sleep() throws InterruptedException {
        if (sleep > 0L) {
            sleepQueue.add(Thread.currentThread());
            try {
                Thread.sleep(sleep);
            }
            catch (InterruptedException e) {
                // 队列中还存在，任务被暂停
                if (sleepQueue.contains(Thread.currentThread())) {
                    throw e;
                }
                // 被唤醒
                else {
                    sleep = 0L;
                }
            }
            finally {
                // 避免休眠结束发生中断
                //noinspection ResultOfMethodCallIgnored
                Thread.interrupted();
                sleepQueue.remove(Thread.currentThread());
            }
        }
    }

}
