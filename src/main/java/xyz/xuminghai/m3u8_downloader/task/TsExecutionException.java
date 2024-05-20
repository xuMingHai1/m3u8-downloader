package xyz.xuminghai.m3u8_downloader.task;

import lombok.Getter;
import xyz.xuminghai.m3u8_downloader.m3u8.TsFile;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * 2024/5/13 下午3:59 星期一<br/>
 * ts任务执行异常
 *
 * @author xuMingHai
 */
@Getter
public class TsExecutionException extends ExecutionException {

    private final List<TsFile> doneTask;

    public TsExecutionException(List<TsFile> doneTask, Throwable cause) {
        super(cause);
        this.doneTask = doneTask;
    }

}
