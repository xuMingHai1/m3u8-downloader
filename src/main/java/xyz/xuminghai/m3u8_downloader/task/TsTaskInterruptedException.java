/*
 * SPDX-FileCopyrightText: 2024 xuMingHai <173535609@qq.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package xyz.xuminghai.m3u8_downloader.task;

import xyz.xuminghai.m3u8_downloader.m3u8.TsFile;

import java.util.List;

/**
 * 2024/5/7 上午4:18 星期二<br/>
 *
 * @author xuMingHai
 */
public class TsTaskInterruptedException extends InterruptedException implements TsTaskException {

    private final List<TsFile> doneTask;

    public TsTaskInterruptedException(List<TsFile> doneTask) {
        this.doneTask = doneTask;
    }

    @Override
    public List<TsFile> getDoneTask() {
        return doneTask;
    }
}
