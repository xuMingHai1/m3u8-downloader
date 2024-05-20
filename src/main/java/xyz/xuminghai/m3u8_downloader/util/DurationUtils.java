package xyz.xuminghai.m3u8_downloader.util;

import java.time.Duration;

/**
 * 2024/4/27 下午11:19 星期六<br/>
 *
 * @author xuMingHai
 */
public final class DurationUtils {

    private DurationUtils() {
    }

    /**
     * 将持续时间转为中文字符串
     *
     * @param duration 持续时间
     * @return 例如：1天，10小时，32分，18秒，789毫秒
     */
    public static String chineseString(Duration duration) {
        final StringBuilder sb = new StringBuilder();

        // 如果天数部分大于0
        final long daysPart = duration.toDaysPart();
        if (daysPart > 0) {
            sb.append(daysPart)
                    .append("天，");
        }
        // 如果小时大于0
        final long hours = duration.toHours();
        if (hours > 0) {
            sb.append(hours % 24)
                    .append("小时，");
        }
        // 如果分钟大于0
        final long minutes = duration.toMinutes();
        if (minutes > 0) {
            sb.append(minutes % 60)
                    .append("分钟，");
        }
        // 如果秒大于0
        final long seconds = duration.toSeconds();
        if (seconds > 0) {
            sb.append(seconds % 60)
                    .append("秒，");
        }
        sb.append(duration.toMillisPart())
                .append("毫秒");

        return sb.toString();
    }
}
