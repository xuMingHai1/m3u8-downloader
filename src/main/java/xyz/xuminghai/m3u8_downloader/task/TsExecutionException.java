/*
 * SPDX-FileCopyrightText: 2024 xuMingHai <173535609@qq.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package xyz.xuminghai.m3u8_downloader.task;

import xyz.xuminghai.m3u8_downloader.m3u8.TsFile;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * 2024/5/13 下午3:59 星期一<br/>
 * ts任务执行异常
 *
 * @author xuMingHai
 */
public class TsExecutionException extends ExecutionException implements TsTaskException {

    private final List<TsFile> doneTask;

    public TsExecutionException(List<TsFile> doneTask, Throwable cause) {
        super(cause);
        this.doneTask = doneTask;
    }

    @Override
    public List<TsFile> getDoneTask() {
        return doneTask;
    }
}
