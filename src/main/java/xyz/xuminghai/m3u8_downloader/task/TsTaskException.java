/*
 * SPDX-FileCopyrightText: 2024 xuMingHai <173535609@qq.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package xyz.xuminghai.m3u8_downloader.task;

import xyz.xuminghai.m3u8_downloader.m3u8.TsFile;

import java.util.List;

/**
 * 2024/6/6 上午1:35 星期四<br/>
 *
 * @author xuMingHai
 */
public interface TsTaskException {

    /**
     * 获取已完成的ts任务
     *
     * @return ts文件
     */
    List<TsFile> getDoneTask();

}
