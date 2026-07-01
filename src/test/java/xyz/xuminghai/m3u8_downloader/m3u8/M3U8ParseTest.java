/*
 * Copyright (C) 2024 xuMingHai 173535609@qq.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 */

package xyz.xuminghai.m3u8_downloader.m3u8;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link M3U8Parse} 的解析行为测试。
 * 覆盖媒体播放列表格式校验、URI 解析、AES-128 key/IV 解析、key 状态切换和异常行号定位。
 */
class M3U8ParseTest {

    private static final URI BASE_URI = URI.create("https://example.com/video/index.m3u8?token=1");

    @TempDir
    private Path tempDir;

    /**
     * 验证普通点播列表可以解析为媒体片段列表。
     * 重点覆盖相对 URI 解析、片段序号递增，以及未加密片段不会共享同一个可变 Key 实例。
     */
    @Test
    void shouldParsePlainVodPlaylistAndResolveSegmentUris() throws Exception {
        final Path path = writePlaylist("""
                #EXTM3U
                #EXTINF:10.0,
                000.ts
                #EXTINF:8.0,
                nested/001.ts
                #EXT-X-ENDLIST
                """);

        final List<MediaPlay> playList = M3U8Parse.parse(BASE_URI, path);

        assertThat(playList).hasSize(2);
        assertThat(playList)
                .extracting(MediaPlay::uri)
                .containsExactly(
                        URI.create("https://example.com/video/000.ts"),
                        URI.create("https://example.com/video/nested/001.ts")
                );
        assertThat(playList)
                .extracting(MediaPlay::sequence)
                .containsExactly(0, 1);
        assertThat(playList)
                .extracting(mediaPlay -> mediaPlay.key().getMethod())
                .containsExactly(KeyMethodEnum.NONE, KeyMethodEnum.NONE);
        assertThat(playList.get(0).key()).isNotSameAs(playList.get(1).key());
    }

    /**
     * 验证字符串入口可以容忍 UTF-8 BOM、空行和行首尾空白。
     * 该用例特意不用文本块，避免尾随空格和首字符 BOM 在缩进处理时变得不直观。
     */
    @Test
    void shouldParseStringContentWithBomBlankLinesAndLineWhitespace() throws Exception {
        final String content = "\uFEFF#EXTM3U\n"
                + "\n"
                + "  #EXTINF:10.0,  \n"
                + "  000.ts  \n"
                + "  #EXT-X-ENDLIST  \n";

        final List<MediaPlay> playList = M3U8Parse.parse(BASE_URI, content);

        assertThat(playList).hasSize(1);
        assertThat(playList.getFirst().uri()).isEqualTo(URI.create("https://example.com/video/000.ts"));
    }

    /**
     * 验证 AES-128 的显式密钥 URI 和 IV 可以被正确解析。
     * IV 使用 0x 前缀的 16 字节十六进制文本。
     */
    @Test
    void shouldParseAes128KeyWithExplicitIv() throws Exception {
        final Path path = writePlaylist("""
                #EXTM3U
                #EXT-X-KEY:METHOD=AES-128,URI="keys/key.bin",IV=0x000102030405060708090a0b0c0d0e0f
                #EXTINF:10.0,
                000.ts
                #EXT-X-ENDLIST
                """);

        final List<MediaPlay> playList = M3U8Parse.parse(BASE_URI, path);

        final M3U8Key key = playList.getFirst().key();
        assertThat(key.getMethod()).isEqualTo(KeyMethodEnum.AES_128);
        assertThat(key.getUri()).isEqualTo(URI.create("https://example.com/video/keys/key.bin"));
        assertThat(key.getIv()).containsExactly(explicitIv());
    }

    /**
     * 验证 EXT-X-KEY 属性解析不会把引号内的逗号当作属性分隔符。
     * 这是 key URI 文件名中包含逗号时的回归保护。
     */
    @Test
    void shouldParseQuotedKeyUriContainingComma() throws Exception {
        final List<MediaPlay> playList = M3U8Parse.parse(BASE_URI, """
                #EXTM3U
                #EXT-X-KEY:METHOD=AES-128,URI="keys/key,part.bin",IV=0x000102030405060708090a0b0c0d0e0f
                #EXTINF:10.0,
                000.ts
                #EXT-X-ENDLIST
                """);

        assertThat(playList.getFirst().key().getUri())
                .isEqualTo(URI.create("https://example.com/video/keys/key,part.bin"));
    }

    /**
     * 验证 EXT-X-KEY 属性名、等号和值两侧的空白可以被忽略。
     * 同时确认 METHOD、URI 和 IV 三个字段都仍然按预期解析。
     */
    @Test
    void shouldParseKeyAttributesWithWhitespace() throws Exception {
        final List<MediaPlay> playList = M3U8Parse.parse(BASE_URI, """
                #EXTM3U
                #EXT-X-KEY: METHOD = AES-128 , URI = "keys/key.bin" , IV = 0x000102030405060708090a0b0c0d0e0f
                #EXTINF:10.0,
                000.ts
                #EXT-X-ENDLIST
                """);

        final M3U8Key key = playList.getFirst().key();
        assertThat(key.getMethod()).isEqualTo(KeyMethodEnum.AES_128);
        assertThat(key.getUri()).isEqualTo(URI.create("https://example.com/video/keys/key.bin"));
        assertThat(key.getIv()).containsExactly(explicitIv());
    }

    /**
     * 验证 AES-128 未声明 IV 时，会按媒体序列号生成 16 字节大端 IV。
     * 第二个片段应使用递增后的媒体序列号。
     */
    @Test
    void shouldUseMediaSequenceAsDefaultIv() throws Exception {
        final Path path = writePlaylist("""
                #EXTM3U
                #EXT-X-MEDIA-SEQUENCE:42
                #EXT-X-KEY:METHOD=AES-128,URI="key.bin"
                #EXTINF:10.0,
                000.ts
                #EXTINF:10.0,
                001.ts
                #EXT-X-ENDLIST
                """);

        final List<MediaPlay> playList = M3U8Parse.parse(BASE_URI, path);

        assertThat(playList).hasSize(2);
        assertThat(playList.getFirst().key().getIv()).containsExactly(sequenceIv(42));
        assertThat(playList.get(1).key().getIv()).containsExactly(sequenceIv(43));
    }

    /**
     * 验证 EXT-X-DISCONTINUITY 本身不会改变当前密钥。
     * 如果 discontinuity 后没有新的 EXT-X-KEY，后续片段应继续沿用当前 key 模板。
     */
    @Test
    void shouldKeepCurrentKeyAcrossDiscontinuityWhenKeyTagDoesNotChange() throws Exception {
        final Path path = writePlaylist("""
                #EXTM3U
                #EXT-X-KEY:METHOD=AES-128,URI="key-1.bin",IV=0x00000000000000000000000000000001
                #EXTINF:10.0,
                000.ts
                #EXT-X-DISCONTINUITY
                #EXTINF:10.0,
                001.ts
                #EXT-X-ENDLIST
                """);

        final List<MediaPlay> playList = M3U8Parse.parse(BASE_URI, path);

        assertThat(playList).hasSize(2);
        assertThat(playList)
                .extracting(mediaPlay -> mediaPlay.key().getUri())
                .containsExactly(
                        URI.create("https://example.com/video/key-1.bin"),
                        URI.create("https://example.com/video/key-1.bin")
                );
        assertThat(playList.get(0).key()).isNotSameAs(playList.get(1).key());
        assertThat(playList.get(0).key().getIv()).containsExactly(playList.get(1).key().getIv());
    }

    /**
     * 验证 discontinuity 后如果出现新的 EXT-X-KEY，后续片段会使用新 key。
     * 该用例区分 discontinuity 标签和 key 标签变更的职责。
     */
    @Test
    void shouldUseNewKeyAfterKeyTagFollowingDiscontinuity() throws Exception {
        final Path path = writePlaylist("""
                #EXTM3U
                #EXT-X-KEY:METHOD=AES-128,URI="key-1.bin",IV=0x00000000000000000000000000000001
                #EXTINF:10.0,
                000.ts
                #EXT-X-DISCONTINUITY
                #EXT-X-KEY:METHOD=AES-128,URI="key-2.bin",IV=0x00000000000000000000000000000002
                #EXTINF:10.0,
                001.ts
                #EXT-X-ENDLIST
                """);

        final List<MediaPlay> playList = M3U8Parse.parse(BASE_URI, path);

        assertThat(playList).hasSize(2);
        assertThat(playList.get(0).key().getUri()).isEqualTo(URI.create("https://example.com/video/key-1.bin"));
        assertThat(playList.get(0).key().getIv()[15]).isEqualTo((byte) 0x01);
        assertThat(playList.get(1).key().getUri()).isEqualTo(URI.create("https://example.com/video/key-2.bin"));
        assertThat(playList.get(1).key().getIv()[15]).isEqualTo((byte) 0x02);
    }

    /**
     * 验证播放列表中途出现新的 EXT-X-KEY 时，后续片段会切换到新 key。
     * 这是普通 key 轮换场景，不依赖 EXT-X-DISCONTINUITY。
     */
    @Test
    void shouldUseNewKeyWhenKeyTagChangesBeforeSegment() throws Exception {
        final Path path = writePlaylist("""
                #EXTM3U
                #EXT-X-KEY:METHOD=AES-128,URI="key-1.bin",IV=0x00000000000000000000000000000001
                #EXTINF:10.0,
                000.ts
                #EXT-X-KEY:METHOD=AES-128,URI="key-2.bin",IV=0x00000000000000000000000000000002
                #EXTINF:10.0,
                001.ts
                #EXT-X-ENDLIST
                """);

        final List<MediaPlay> playList = M3U8Parse.parse(BASE_URI, path);

        assertThat(playList).hasSize(2);
        assertThat(playList.get(0).key().getUri()).isEqualTo(URI.create("https://example.com/video/key-1.bin"));
        assertThat(playList.get(1).key().getUri()).isEqualTo(URI.create("https://example.com/video/key-2.bin"));
    }

    /**
     * 验证 METHOD=NONE 可以从 AES-128 状态切回未加密状态。
     * 防止后续片段错误沿用前一个 AES key。
     */
    @Test
    void shouldUseNoEncryptionAfterMethodNone() throws Exception {
        final Path path = writePlaylist("""
                #EXTM3U
                #EXT-X-KEY:METHOD=AES-128,URI="key.bin",IV=0x00000000000000000000000000000001
                #EXTINF:10.0,
                000.ts
                #EXT-X-KEY:METHOD=NONE
                #EXTINF:10.0,
                001.ts
                #EXT-X-ENDLIST
                """);

        final List<MediaPlay> playList = M3U8Parse.parse(BASE_URI, path);

        assertThat(playList)
                .extracting(mediaPlay -> mediaPlay.key().getMethod())
                .containsExactly(KeyMethodEnum.AES_128, KeyMethodEnum.NONE);
        assertThat(playList.get(1).key().getUri()).isNull();
        assertThat(playList.get(1).key().getIv()).isNull();
    }

    /**
     * 验证缺少 EXT-X-ENDLIST 的播放列表会被拒绝。
     * 当前解析器只接受带结束标签的点播列表。
     */
    @Test
    void shouldRejectPlaylistWithoutEndList() throws Exception {
        final Path path = writePlaylist("""
                #EXTM3U
                #EXTINF:10.0,
                000.ts
                """);

        assertThatThrownBy(() -> M3U8Parse.parse(BASE_URI, path))
                .isInstanceOf(M3U8ParseException.class)
                .hasMessageContaining("不是m3u8文件");
    }

    /**
     * 验证 master playlist 会被明确拒绝。
     * 当前解析器只处理媒体播放列表，不处理多码率主播放列表。
     */
    @Test
    void shouldRejectMasterPlaylist() {
        assertThatThrownBy(() -> M3U8Parse.parse(BASE_URI, """
                #EXTM3U
                #EXT-X-STREAM-INF:BANDWIDTH=1280000,RESOLUTION=640x360
                low/index.m3u8
                """))
                .isInstanceOf(M3U8ParseException.class)
                .hasMessageContaining("暂不支持多码率主播放列表");
    }

    /**
     * 验证没有任何媒体片段 URI 的播放列表会被拒绝。
     * 只包含合法标签但没有片段时，不应返回空播放列表。
     */
    @Test
    void shouldRejectPlaylistWithoutMediaSegments() {
        assertThatThrownBy(() -> M3U8Parse.parse(BASE_URI, """
                #EXTM3U
                #EXT-X-VERSION:3
                #EXT-X-ENDLIST
                """))
                .isInstanceOf(M3U8ParseException.class)
                .hasMessageContaining("不存在点播列表");
    }

    /**
     * 验证非法 IV 会带出 EXT-X-KEY 所在行号。
     * 行号用于帮助定位播放列表中的具体错误标签。
     */
    @Test
    void shouldIncludeLineNumberWhenKeyIvIsInvalid() {
        assertThatThrownBy(() -> M3U8Parse.parse(BASE_URI, """
                #EXTM3U
                #EXT-X-KEY:METHOD=AES-128,URI="key.bin",IV=invalid
                #EXTINF:10.0,
                000.ts
                #EXT-X-ENDLIST
                """))
                .isInstanceOf(M3U8ParseException.class)
                .hasMessageContaining("第2行")
                .hasMessageContaining("EXT-X-KEY.IV格式错误");
    }

    /**
     * 验证非法 EXT-X-MEDIA-SEQUENCE 会带出标签所在行号。
     * 非数字序列号不应继续参与默认 IV 计算。
     */
    @Test
    void shouldIncludeLineNumberWhenMediaSequenceIsInvalid() {
        assertThatThrownBy(() -> M3U8Parse.parse(BASE_URI, """
                #EXTM3U
                #EXT-X-MEDIA-SEQUENCE:abc
                #EXTINF:10.0,
                000.ts
                #EXT-X-ENDLIST
                """))
                .isInstanceOf(M3U8ParseException.class)
                .hasMessageContaining("第2行")
                .hasMessageContaining("EXT-X-MEDIA-SEQUENCE格式错误");
    }

    /**
     * 验证 AES-128 缺少 URI 属性时会带出 EXT-X-KEY 所在行号。
     * AES-128 必须有可解析的 key URI。
     */
    @Test
    void shouldIncludeLineNumberWhenKeyUriIsMissing() {
        assertThatThrownBy(() -> M3U8Parse.parse(BASE_URI, """
                #EXTM3U
                #EXT-X-KEY:METHOD=AES-128,IV=0x000102030405060708090a0b0c0d0e0f
                #EXTINF:10.0,
                000.ts
                #EXT-X-ENDLIST
                """))
                .isInstanceOf(M3U8ParseException.class)
                .hasMessageContaining("第2行")
                .hasMessageContaining("EXT-X-KEY.URI格式错误");
    }

    /**
     * 验证 EXT-X-KEY 属性中未闭合的引号会带出标签所在行号。
     * 该场景防止 quoted 属性解析吞掉后续字段。
     */
    @Test
    void shouldIncludeLineNumberWhenKeyAttributeQuoteIsUnclosed() {
        assertThatThrownBy(() -> M3U8Parse.parse(BASE_URI, """
                #EXTM3U
                #EXT-X-KEY:METHOD=AES-128,URI="key.bin,IV=0x000102030405060708090a0b0c0d0e0f
                #EXTINF:10.0,
                000.ts
                #EXT-X-ENDLIST
                """))
                .isInstanceOf(M3U8ParseException.class)
                .hasMessageContaining("第2行")
                .hasMessageContaining("EXT-X-KEY属性引号未闭合");
    }

    private static byte[] explicitIv() {
        return new byte[]{
                0x00, 0x01, 0x02, 0x03,
                0x04, 0x05, 0x06, 0x07,
                0x08, 0x09, 0x0a, 0x0b,
                0x0c, 0x0d, 0x0e, 0x0f
        };
    }

    private static byte[] sequenceIv(long sequence) {
        return ByteBuffer.allocate(16).putLong(8, sequence).array();
    }

    private Path writePlaylist(String content) throws IOException {
        final Path path = tempDir.resolve("index.m3u8");
        Files.writeString(path, content);
        return path;
    }
}
