package xyz.xuminghai.m3u8_downloader.task;

import java.net.ProxySelector;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;

/**
 * 2024/4/29 下午2:10 星期一<br/>
 *
 * @author xuMingHai
 */
public record M3U8(URI m3u8Uri,
                   ProxySelector proxySelector,
                   Path filePath,
                   Path downloadTempDirPath,
                   Duration timeout) {

}
