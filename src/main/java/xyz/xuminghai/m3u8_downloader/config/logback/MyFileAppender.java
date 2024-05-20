package xyz.xuminghai.m3u8_downloader.config.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import xyz.xuminghai.m3u8_downloader.config.CommonData;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

/**
 * 2024/3/29 14:43 星期五<br/>
 * 自定义临时日志文件合并
 *
 * @author xuMingHai
 */
public class MyFileAppender extends FileAppender<ILoggingEvent> {

    private static final Path
            // 应用日志文件目录
            APP_LOG_DIR = CommonData.APP_DIR.resolve("log"),
    // 日志文件路径
    LOG_FILE_PATH = APP_LOG_DIR.resolve("log.log"),
    // 临时日志文件位置
    TMP_LOG_FILE_PATH = APP_LOG_DIR.resolve("log-" + Instant.now().getEpochSecond() + ".log");

    MyFileAppender() {
        super.setName("myFileAppender");
        // 日志文件
        super.setFile(TMP_LOG_FILE_PATH.toString());
        // 截断日志
        super.setAppend(false);
    }

    @Override
    public void stop() {
        // 已经关闭
        if (!isStarted()) {
            return;
        }
        super.stop();
        try (FileChannel sourceFileChannel = FileChannel.open(TMP_LOG_FILE_PATH,
                StandardOpenOption.READ,
                StandardOpenOption.DELETE_ON_CLOSE,
                StandardOpenOption.SYNC);
             FileChannel targetFileChannel = FileChannel.open(LOG_FILE_PATH,
                     StandardOpenOption.WRITE,
                     StandardOpenOption.TRUNCATE_EXISTING,
                     StandardOpenOption.CREATE,
                     StandardOpenOption.SYNC)) {
            sourceFileChannel.transferTo(0L, Long.MAX_VALUE, targetFileChannel);
        }
        catch (IOException e) {
            super.addError("合并日志异常", e);
        }
    }


}
