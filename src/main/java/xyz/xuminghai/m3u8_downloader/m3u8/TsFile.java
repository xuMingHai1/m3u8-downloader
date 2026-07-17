/*
 * SPDX-FileCopyrightText: 2024 xuMingHai <173535609@qq.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package xyz.xuminghai.m3u8_downloader.m3u8;

import java.nio.file.Path;

/**
 * 2024/5/7 下午4:03 星期二<br/>
 *
 * @author xuMingHai
 */
public record TsFile(int sequence,
                     Path path) {
}
