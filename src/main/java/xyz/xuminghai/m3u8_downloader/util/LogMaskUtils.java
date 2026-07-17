/*
 * SPDX-FileCopyrightText: 2024 xuMingHai <173535609@qq.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package xyz.xuminghai.m3u8_downloader.util;

import java.net.URI;

public final class LogMaskUtils {

    public static final String MASK = "***";

    private LogMaskUtils() {
    }

    public static String uri(URI uri) {
        if (uri == null) {
            return "null";
        }
        if (uri.isOpaque()) {
            return uri.getScheme() == null ? MASK : uri.getScheme() + ":" + MASK;
        }

        final StringBuilder stringBuilder = new StringBuilder();
        if (uri.getScheme() != null) {
            stringBuilder.append(uri.getScheme()).append(':');
        }
        if (uri.getRawAuthority() != null) {
            stringBuilder.append("//").append(authority(uri.getRawAuthority()));
        }
        if (uri.getRawPath() != null) {
            stringBuilder.append(uri.getRawPath());
        }
        if (uri.getRawQuery() != null) {
            stringBuilder.append('?').append(MASK);
        }
        if (uri.getRawFragment() != null) {
            stringBuilder.append('#').append(MASK);
        }
        if (stringBuilder.isEmpty()) {
            return uri.toString();
        }
        return stringBuilder.toString();
    }

    private static String authority(String rawAuthority) {
        final int userInfoEndIndex = rawAuthority.lastIndexOf('@');
        if (userInfoEndIndex < 0) {
            return rawAuthority;
        }
        return MASK + rawAuthority.substring(userInfoEndIndex);
    }

}
