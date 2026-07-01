/*
 * Copyright (C) 2024 xuMingHai 173535609@qq.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 */

package xyz.xuminghai.m3u8_downloader.util;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

class LogMaskUtilsTest {

    @Test
    void shouldMaskUriQueryFragmentAndUserInfo() {
        final URI uri = URI.create("https://user:password@example.com:8443/path/index.m3u8?token=123#media");

        final String logUri = LogMaskUtils.uri(uri);

        assertThat(logUri).isEqualTo("https://***@example.com:8443/path/index.m3u8?***#***");
    }

    @Test
    void shouldMaskOpaqueUriSchemeSpecificPart() {
        final URI uri = URI.create("mailto:user@example.com?subject=secret");

        final String logUri = LogMaskUtils.uri(uri);

        assertThat(logUri).isEqualTo("mailto:***");
    }

    @Test
    void shouldKeepRelativePathAndMaskQuery() {
        final URI uri = URI.create("video/000.ts?sign=secret");

        final String logUri = LogMaskUtils.uri(uri);

        assertThat(logUri).isEqualTo("video/000.ts?***");
    }

}
