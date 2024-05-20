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
