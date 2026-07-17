/*
 * SPDX-FileCopyrightText: 2024 xuMingHai <173535609@qq.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package xyz.xuminghai.m3u8_downloader.m3u8;

import java.net.URI;

/**
 * 2024/4/26 下午10:59 星期五<br/>
 *
 * @author xuMingHai
 */
public record MediaPlay(URI uri,
                        int sequence,
                        M3U8Key key) {
}
