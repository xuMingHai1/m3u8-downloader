package xyz.xuminghai.m3u8_downloader.http;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;

/**
 * 2024/5/26 下午3:14 星期日<br/>
 *
 * @author xuMingHai
 */
public class DownloadPathSubscriber implements HttpResponse.BodySubscriber<Path> {

    private static final ByteBuffer[] EMPTY_BUFFER = new ByteBuffer[0];

    private final Path file;
    private final OpenOption[] options;
    private final LongAdder downloadByteCount;

    private final CompletableFuture<Path> result = new CompletableFuture<>();
    private final AtomicBoolean subscribed = new AtomicBoolean();
    private volatile Flow.Subscription subscription;
    private volatile FileChannel out;

    public DownloadPathSubscriber(Path file, OpenOption[] options, LongAdder downloadByteCount) {
        this.file = file;
        this.options = options;
        this.downloadByteCount = downloadByteCount;
    }


    @Override
    public CompletionStage<Path> getBody() {
        return result;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        Objects.requireNonNull(subscription);
        if (!subscribed.compareAndSet(false, true)) {
            subscription.cancel();
            return;
        }

        this.subscription = subscription;
        try {
            out = FileChannel.open(file, options);
        }
        catch (IOException ioe) {
            result.completeExceptionally(ioe);
            subscription.cancel();
            return;
        }
        subscription.request(1);
    }

    @Override
    public void onNext(List<ByteBuffer> items) {
        ByteBuffer[] buffers = items.toArray(EMPTY_BUFFER);
        // 统计下载速度
        downloadByteCount.add(remaining(buffers));
        try {
            do {
                out.write(buffers);
            }
            while (hasRemaining(buffers));
        }
        catch (IOException ex) {
            close();
            subscription.cancel();
            result.completeExceptionally(ex);
        }
        subscription.request(1);
    }

    @Override
    public void onError(Throwable throwable) {
        result.completeExceptionally(throwable);
        close();
    }

    @Override
    public void onComplete() {
        close();
        result.complete(file);
    }

    private long remaining(ByteBuffer[] buffers) {
        long sum = 0;
        for (ByteBuffer buffer : buffers) {
            sum += buffer.remaining();
        }
        return sum;
    }

    private boolean hasRemaining(ByteBuffer[] buffers) {
        for (ByteBuffer buf : buffers) {
            if (buf.hasRemaining())
                return true;
        }
        return false;
    }

    private void close() {
        try {
            out.close();
        }
        catch (IOException _) {

        }
    }
}
