/*
 * SPDX-FileCopyrightText: 2024 xuMingHai <173535609@qq.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package xyz.xuminghai.m3u8_downloader.util;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link LogMaskUtils} 的日志脱敏行为测试。
 * 覆盖 URI 的 userinfo、query、fragment、opaque URI、相对 URI 和空值处理。
 */
class LogMaskUtilsTest {

    /**
     * 验证完整层级 URI 中的 userinfo、query 和 fragment 会被脱敏。
     * host、port 和 path 需要保留，方便日志定位请求目标。
     */
    @Test
    void shouldMaskUriQueryFragmentAndUserInfo() {
        final URI uri = URI.create("https://user:password@example.com:8443/path/index.m3u8?token=123#media");

        final String logUri = LogMaskUtils.uri(uri);

        assertThat(logUri).isEqualTo("https://***@example.com:8443/path/index.m3u8?***#***");
    }

    /**
     * 验证 opaque URI 不输出 scheme-specific part。
     * 这类 URI 没有可拆分 path/query 结构，只保留 scheme 用于识别类型。
     */
    @Test
    void shouldMaskOpaqueUriSchemeSpecificPart() {
        final URI uri = URI.create("mailto:user@example.com?subject=secret");

        final String logUri = LogMaskUtils.uri(uri);

        assertThat(logUri).isEqualTo("mailto:***");
    }

    /**
     * 验证相对 URI 会保留路径并脱敏 query。
     * m3u8 中的片段地址常见为相对路径。
     */
    @Test
    void shouldKeepRelativePathAndMaskQuery() {
        final URI uri = URI.create("video/000.ts?sign=secret");

        final String logUri = LogMaskUtils.uri(uri);

        assertThat(logUri).isEqualTo("video/000.ts?***");
    }

    /**
     * 验证没有敏感组成部分的 URI 会按原始可定位信息输出。
     * 无 query、fragment、userinfo 时不应过度脱敏。
     */
    @Test
    void shouldKeepUriWithoutSensitiveParts() {
        final URI uri = URI.create("https://example.com/video/index.m3u8");

        final String logUri = LogMaskUtils.uri(uri);

        assertThat(logUri).isEqualTo("https://example.com/video/index.m3u8");
    }

    /**
     * 验证 network-path URI 同样会脱敏 userinfo 和 query。
     * 这类 URI 没有 scheme，但仍然可能包含 authority。
     */
    @Test
    void shouldMaskNetworkPathUriUserInfoAndQuery() {
        final URI uri = URI.create("//user:password@example.com/video/index.m3u8?token=123");

        final String logUri = LogMaskUtils.uri(uri);

        assertThat(logUri).isEqualTo("//***@example.com/video/index.m3u8?***");
    }

    /**
     * 验证相对 URI 的 fragment 会被脱敏。
     * fragment 可能携带定位或鉴权信息，不应直接进入日志。
     */
    @Test
    void shouldMaskRelativeUriFragment() {
        final URI uri = URI.create("video/000.ts#secret");

        final String logUri = LogMaskUtils.uri(uri);

        assertThat(logUri).isEqualTo("video/000.ts#***");
    }

    /**
     * 验证 null URI 会输出稳定的占位文本。
     * 该行为用于避免日志构造时额外处理空值。
     */
    @Test
    void shouldReturnNullTextWhenUriIsNull() {
        final String logUri = LogMaskUtils.uri(null);

        assertThat(logUri).isEqualTo("null");
    }

}
