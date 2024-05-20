package xyz.xuminghai.m3u8_downloader.m3u8;

import java.nio.file.Path;

/**
 * 2024/5/7 下午4:03 星期二<br/>
 *
 * @author xuMingHai
 */
public record TsFile(int sequence,
                     Path path) {
}
