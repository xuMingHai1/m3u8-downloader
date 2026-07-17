/*
 * SPDX-FileCopyrightText: 2024 xuMingHai <173535609@qq.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package xyz.xuminghai.m3u8_downloader.m3u8;

import lombok.extern.slf4j.Slf4j;
import xyz.xuminghai.m3u8_downloader.util.LogMaskUtils;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * 2024/4/21 上午1:08 星期日<br/>
 * 简单的获取点播列表
 * <p>
 * 依据<a href="https://datatracker.ietf.org/doc/html/rfc8216">rfc8216</a>
 *
 * @author xuMingHai
 */
@Slf4j
public final class M3U8Parse {

    private static final String
            START = "#EXTM3U",
            END = "#EXT-X-ENDLIST",
            EXT_X_KEY = "#EXT-X-KEY:",
            EXT_X_STREAM_INF = "#EXT-X-STREAM-INF:",
            SEQUENCE = "#EXT-X-MEDIA-SEQUENCE:";

    private static final int MAX_M3U8_FILE_SIZE = 5 << 20;

    /**
     * 解析字符串内容，转为点播链接URI地址
     *
     * @return M3U8类
     * @throws M3U8ParseException 解析异常
     */
    public static List<MediaPlay> parse(URI uri, Path path) throws M3U8ParseException, IOException {
        log.debug("开始解析m3u8文件");
        if (uri == null || path == null) {
            throw new M3U8ParseException("不存在文件");
        }
        // 文件大于5MB
        if (Files.size(path) > MAX_M3U8_FILE_SIZE) {
            throw new M3U8ParseException("m3u8文件大于5MB");
        }
        // 判断是否为M3U8格式
        final String content = Files.readString(path);
        return parse(uri, content);
    }

    /**
     * 解析字符串内容，转为点播链接URI地址
     *
     * @return M3U8类
     * @throws M3U8ParseException 解析异常
     */
    public static List<MediaPlay> parse(URI uri, String content) throws M3U8ParseException {
        log.debug("开始解析m3u8内容");
        if (uri == null || content == null) {
            throw new M3U8ParseException("不存在m3u8内容");
        }
        final List<PlaylistLine> list = lines(content);
        if (list.isEmpty()) {
            throw new M3U8ParseException("m3u8内容为空");
        }
        if (!START.equals(list.getFirst().content())) {
            throw new M3U8ParseException("不是m3u8文件，START = " + list.getFirst().content()
                    + ", END = " + list.getLast().content());
        }
        if (containsMasterPlaylist(list)) {
            throw new M3U8ParseException("暂不支持多码率主播放列表");
        }
        if (!END.equals(list.getLast().content())) {
            throw new M3U8ParseException("不是m3u8文件，START = " + list.getFirst().content()
                    + ", END = " + list.getLast().content());
        }
        log.debug("是m3u8文件格式");

        return playList(uri, list);
    }

    private static List<PlaylistLine> lines(String content) {
        final List<String> rawLines = content.lines().toList();
        final List<PlaylistLine> list = new ArrayList<>(rawLines.size());
        for (int i = 0; i < rawLines.size(); i++) {
            String line = rawLines.get(i).strip();
            if (i == 0 && line.startsWith("\uFEFF")) {
                line = line.substring(1);
            }
            if (!line.isEmpty()) {
                list.add(new PlaylistLine(i + 1, line));
            }
        }
        return list;
    }

    private static boolean containsMasterPlaylist(List<PlaylistLine> list) {
        return list.stream().anyMatch(line -> line.content().startsWith(EXT_X_STREAM_INF));
    }

    private static KeyTemplate key(URI uri, PlaylistLine extXKey) throws M3U8ParseException {
        log.debug("存在{}，开始解析{}标签", EXT_X_KEY, EXT_X_KEY);
        final KeyMethodEnum keyMethod;
        final URI keyUri;
        final byte[] iv;
        final Map<String, String> map = attributes(extXKey.content().substring(EXT_X_KEY.length()), extXKey.number());

        keyMethod = KeyMethodEnum.of(map.get("METHOD"));
        log.debug("KeyMethodEnum = {}", keyMethod);

        // URI不能为null, 除非 METHOD 为 NONE
        if (keyMethod == KeyMethodEnum.NONE) {
            return KeyTemplate.NONE;
        }
        else {
            // 获Key的URI
            final String uriString = map.get("URI");
            if (uriString == null || uriString.isBlank()) {
                throw lineException(extXKey.number(), "EXT-X-KEY.URI格式错误，URI = " + uriString);
            }
            // 去掉双引号，基于M3U8地址转换
            keyUri = uri.resolve(unquote(uriString, extXKey.number()));
            log.atDebug().setMessage("KeyUri = {}")
                    .addArgument(() -> LogMaskUtils.uri(keyUri)).log();
            // 获取IV（初始化向量）
            final String ivString = map.get("IV");
            log.atDebug().setMessage("IvString = {}")
                    .addArgument(ivString).log();
            iv = iv(ivString, extXKey.number());
        }

        return new KeyTemplate(keyMethod, keyUri, iv);
    }

    private static Map<String, String> attributes(String attributes, int lineNumber) throws M3U8ParseException {
        final Map<String, String> map = new HashMap<>();
        final StringBuilder attribute = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < attributes.length(); i++) {
            final char c = attributes.charAt(i);
            if (c == '"') {
                quoted = !quoted;
            }
            if (c == ',' && !quoted) {
                putAttribute(map, attribute.toString(), lineNumber);
                attribute.setLength(0);
            }
            else {
                attribute.append(c);
            }
        }
        if (quoted) {
            throw lineException(lineNumber, "EXT-X-KEY属性引号未闭合");
        }
        putAttribute(map, attribute.toString(), lineNumber);
        return map;
    }

    private static void putAttribute(Map<String, String> map, String attribute, int lineNumber) throws M3U8ParseException {
        final String[] attr = attribute.split("=", 2);
        if (attr.length != 2 || attr[0].isBlank()) {
            throw lineException(lineNumber, "EXT-X-KEY错误，attribute = " + attribute);
        }
        map.put(attr[0].strip(), attr[1].strip());
    }

    private static String unquote(String value, int lineNumber) throws M3U8ParseException {
        if (value.length() < 2 || value.charAt(0) != '"' || value.charAt(value.length() - 1) != '"') {
            throw lineException(lineNumber, "EXT-X-KEY.URI格式错误，URI = " + value);
        }
        return value.substring(1, value.length() - 1);
    }

    private static byte[] iv(String ivString, int lineNumber) throws M3U8ParseException {
        if (ivString == null || ivString.isBlank()) {
            return null;
        }
        // 应该为16进制表示的16字节数据
        if ((!ivString.startsWith("0x") && !ivString.startsWith("0X")) || ivString.length() != 34) {
            throw lineException(lineNumber, "EXT-X-KEY.IV格式错误，IV = " + ivString);
        }

        final byte[] iv = new byte[16];
        final String hexString = ivString.substring(2);
        // 16进制字符序列转为16字节数组
        for (int i = 0; i < hexString.length(); i += 2) {
            final int high = Character.digit(hexString.charAt(i), 16);
            final int low = Character.digit(hexString.charAt(i + 1), 16);
            if (high < 0 || low < 0) {
                throw lineException(lineNumber, "EXT-X-KEY.IV格式错误，IV = " + ivString);
            }
            iv[i / 2] = (byte) ((high << 4) + low);
        }
        log.atDebug().setMessage("IV array = {}")
                .addArgument(() -> Arrays.toString(iv))
                .log();
        return iv;
    }

    private static byte[] sequenceIv(long sequence) {
        final byte[] iv = ByteBuffer.allocate(16).putLong(8, sequence).array();
        log.atDebug().setMessage("SequenceIV = {}")
                .addArgument(() -> Arrays.toString(iv))
                .log();
        return iv;
    }

    private static long sequence(PlaylistLine line) throws M3U8ParseException {
        final String number = line.content().substring(SEQUENCE.length());
        log.debug("{} = {}", SEQUENCE, number);
        try {
            return Long.parseLong(number);
        }
        catch (NumberFormatException _) {
            throw lineException(line.number(), "EXT-X-MEDIA-SEQUENCE格式错误，SEQUENCE = " + number);
        }
    }


    private static List<MediaPlay> playList(URI uri, List<PlaylistLine> list) throws M3U8ParseException {
        final List<MediaPlay> playList = new ArrayList<>(list.size());
        KeyTemplate keyTemplate = KeyTemplate.NONE;
        long mediaSequence = 0L;

        for (PlaylistLine line : list) {
            if (line.content().startsWith(SEQUENCE)) {
                mediaSequence = sequence(line);
                continue;
            }
            if (line.content().startsWith(EXT_X_KEY)) {
                keyTemplate = key(uri, line);
                continue;
            }
            // 媒体播放地址
            if (!line.content().startsWith("#")) {
                playList.add(new MediaPlay(uri.resolve(line.content()), playList.size(),
                        keyTemplate.toM3U8Key(mediaSequence)));
                mediaSequence++;
            }
        }
        if (playList.isEmpty()) {
            throw new M3U8ParseException("不存在点播列表");
        }
        log.atTrace().setMessage("""
                        PlayList.Size = {}
                        PlayList = {}
                        """)
                .addArgument(playList::size)
                .addArgument(playList)
                .log();
        return playList;
    }

    private static M3U8ParseException lineException(int lineNumber, String message) {
        return new M3U8ParseException("第" + lineNumber + "行：" + message);
    }

    private record PlaylistLine(int number, String content) {
    }

    private record KeyTemplate(KeyMethodEnum method, URI uri, byte[] iv) {

        private static final KeyTemplate NONE = new KeyTemplate(KeyMethodEnum.NONE, null, null);

        private M3U8Key toM3U8Key(long mediaSequence) {
            if (method == KeyMethodEnum.NONE) {
                return M3U8Key.none();
            }
            if (iv == null) {
                return new M3U8Key(method, uri, sequenceIv(mediaSequence));
            }
            return new M3U8Key(method, uri, iv.clone());
        }
    }
}
