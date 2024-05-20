package xyz.xuminghai.m3u8_downloader.task;

import lombok.Getter;
import xyz.xuminghai.m3u8_downloader.m3u8.TsFile;

import java.util.List;

/**
 * 2024/5/7 上午4:18 星期二<br/>
 *
 * @author xuMingHai
 */
@Getter
public class TsTaskInterruptedException extends InterruptedException {

    private final List<TsFile> doneTask;

    public TsTaskInterruptedException(List<TsFile> doneTask) {
        this.doneTask = doneTask;
    }
}
