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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class M3U8ParseTest {

    private static final URI BASE_URI = URI.create("https://example.com/video/index.m3u8?token=1");

    @TempDir
    private Path tempDir;

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
        assertThat(key.getIv()).containsExactly(
                (byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x03,
                (byte) 0x04, (byte) 0x05, (byte) 0x06, (byte) 0x07,
                (byte) 0x08, (byte) 0x09, (byte) 0x0a, (byte) 0x0b,
                (byte) 0x0c, (byte) 0x0d, (byte) 0x0e, (byte) 0x0f
        );
    }

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
    }

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
        assertThat(playList.getFirst().key().getIv()).containsExactly(
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x2a
        );
        assertThat(playList.get(1).key().getIv()).containsExactly(
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x2b
        );
    }

    @Test
    void shouldUseNewKeyAfterDiscontinuity() throws Exception {
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

    private Path writePlaylist(String content) throws IOException {
        final Path path = tempDir.resolve("index.m3u8");
        Files.writeString(path, content);
        return path;
    }
}
