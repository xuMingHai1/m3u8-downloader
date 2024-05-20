package xyz.xuminghai.m3u8_downloader.m3u8;

import lombok.extern.slf4j.Slf4j;

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
            EXT_INF = "#EXTINF:",
            EXT_X_KEY = "#EXT-X-KEY:",
            SEQUENCE = "#EXT-X-MEDIA-SEQUENCE:",
    /**
     * EXT-X-DISCONTINUITY 标签指示
     * 其后面的媒体段与其前面的媒体段之间的不连续性
     */
    DISCONTINUITY = "#EXT-X-DISCONTINUITY";

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
        if (Files.size(path) > (5 << 20)) {
            throw new M3U8ParseException("m3u8文件大于5MB");
        }
        // 判断是否为M3U8格式
        final String content = Files.readString(path);
        // 根据行分隔符转为列表
        final List<String> list = content.strip().lines().toList();
        if (!START.equals(list.getFirst()) || !END.equals(list.getLast())) {
            throw new M3U8ParseException("不是m3u8文件，START = " + list.getFirst() + ", END = " + list.getLast());
        }
        log.debug("是m3u8文件格式");

        return playList(uri, list);
    }

    private static M3U8Key key(URI uri, List<String> list, int index, int maxSize) throws M3U8ParseException {
        final String extXKey = list.stream()
                .skip(index)
                .limit(maxSize)
                // 获取#EXT-X-KEY
                .filter(s -> s.startsWith(EXT_X_KEY))
                .findFirst()
                .orElse(null);
        if (extXKey == null) {
            log.debug("不存在{}，KeyMethod = NONE", EXT_X_KEY);
            return M3U8Key.NONE;
        }

        // 存在EXT-X-KEY标签
        log.debug("存在{}，开始解析{}标签", EXT_X_KEY, EXT_X_KEY);
        final String[] attributes = extXKey.substring(EXT_X_KEY.length()).split(",");
        final KeyMethodEnum keyMethod;
        final URI keyUri;
        final byte[] iv;
        final Map<String, String> map = new HashMap<>(attributes.length);

        for (String attribute : attributes) {
            final String[] attr = attribute.split("=");
            if (attr.length != 2) {
                throw new M3U8ParseException("EXT-X-KEY错误，attributes = " + Arrays.toString(attributes));
            }
            map.put(attr[0], attr[1]);
        }

        keyMethod = KeyMethodEnum.of(map.get("METHOD"));
        log.debug("KeyMethodEnum = {}", keyMethod);

        // URI不能为null, 除非 METHOD 为 NONE
        if (keyMethod == KeyMethodEnum.NONE) {
            keyUri = null;
            iv = null;
        }
        else {
            // 获Key的URI
            final String uriString = map.get("URI");
            if (uriString == null || uriString.isBlank()) {
                throw new M3U8ParseException("EXT-X-KEY.URI格式错误，URI = " + uriString);
            }
            // 去掉双引号，基于M3U8地址转换
            keyUri = uri.resolve(uriString.substring(1, uriString.length() - 1));
            log.atDebug().setMessage("KeyUri = {}")
                    .addArgument(keyUri).log();
            // 获取IV（初始化向量）
            String ivString = map.get("IV");
            log.atDebug().setMessage("IvString = {}")
                    .addArgument(ivString).log();
            // 如果为空使用媒体序列号用作 IV
            /*
               方法是将其大端二进制表示形式放入16 字节（128 位）缓冲区和用零填充（左侧）
             */
            if (ivString == null || ivString.isBlank()) {
                log.debug("IV不存在，从媒体序列转换");
                final int sequence = sequence(list, index);
                iv = ByteBuffer.allocate(16).putInt(sequence).array();
                log.atDebug().setMessage("SequenceIV = {}")
                        .addArgument(() -> Arrays.toString(iv))
                        .log();
            }
            else {
                // 应该为16进制表示的16字节数据
                if (ivString.length() != 34) {
                    throw new M3U8ParseException("EXT-X-KEY.IV格式错误，IV = " + ivString);
                }
                // 去除字符串开头的 "0x" 前缀
                ivString = ivString.substring(2);
                iv = new byte[16];
                // 16进制字符序列转为16字节数组
                for (int i = 0; i < ivString.length(); i += 2) {
                    iv[i / 2] = (byte) ((Character.digit(ivString.charAt(i), 16) << 4)// 高4位
                            + Character.digit(ivString.charAt(i + 1), 16));
                }
                log.atDebug().setMessage("IV array = {}")
                        .addArgument(() -> Arrays.toString(iv))
                        .log();
            }
        }

        return new M3U8Key(keyMethod, keyUri, iv);
    }

    private static int sequence(List<String> list, int index) {
        final String number = list.stream()
                .skip(index)
                .filter(s -> s.startsWith(SEQUENCE))
                .findFirst()
                .map(s -> s.substring(SEQUENCE.length()))
                .orElse("0");
        log.debug("{} = {}", SEQUENCE, number);
        return Integer.parseInt(number);
    }


    private static List<MediaPlay> playList(URI uri, List<String> list) throws M3U8ParseException {
        final int size = list.size();
        final List<MediaPlay> playList = new ArrayList<>(size);
        M3U8Key m3u8Key = null;
        for (int i = 0; i < size; i++) {
            if (list.get(i).startsWith(EXT_INF)) {
                m3u8Key = key(uri, list, 0, i);
                break;
            }
        }
        if (m3u8Key == null) {
            throw new M3U8ParseException("不存在点播列表");
        }

        for (int i = 0; i < size; i++) {
            final String line = list.get(i);
            // 不连续的媒体段，从新获取密钥
            if (DISCONTINUITY.equals(line)) {
                m3u8Key = key(uri, list, i, size);
            }
            // 媒体播放地址
            if (!line.isBlank() && !line.startsWith("#")) {
                playList.add(new MediaPlay(uri.resolve(line), playList.size(), m3u8Key));
            }
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
}
