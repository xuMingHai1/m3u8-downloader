/*
 * SPDX-FileCopyrightText: 2024 xuMingHai <173535609@qq.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package xyz.xuminghai.m3u8_downloader.http;

import java.io.IOException;

/**
 * 2024/4/27 上午1:09 星期六<br/>
 *
 * @author xuMingHai
 */
public class HttpStatusCodeException extends IOException {

    public HttpStatusCodeException(String message) {
        super(message);
    }

}
