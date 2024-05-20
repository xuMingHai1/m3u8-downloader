package xyz.xuminghai.m3u8_downloader.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 2024/5/17 下午12:53 星期五<br/>
 *
 * @author xuMingHai
 */
public final class FileSizeUtils {

    private FileSizeUtils() {
    }

    private static final long KB = 1024,
            MB = KB * KB,
            GB = KB * MB;

    private static final BigDecimal KB_DECIMAL = new BigDecimal(KB),
            MB_DECIMAL = new BigDecimal(MB),
            GB_DECIMAL = new BigDecimal(GB);

    public static String convertString(long size) {
        if (size < KB) {
            return size + "B";
        }
        final BigDecimal sizeDecimal = new BigDecimal(size);
        if (size < MB) {
            return quotient(sizeDecimal, KB_DECIMAL) + "KB";
        }
        if (size < GB) {
            return quotient(sizeDecimal, MB_DECIMAL) + "MB";
        }
        return quotient(sizeDecimal, GB_DECIMAL) + "GB";
    }

    private static String quotient(BigDecimal sizeDecimal, BigDecimal divisor) {
        final float value = sizeDecimal.divide(divisor, 2, RoundingMode.HALF_UP).floatValue();
        return String.valueOf(value);
    }

    private static final Pattern PATTERN = Pattern.compile("(\\d+)(KB|MB|GB)?", Pattern.CASE_INSENSITIVE);

    /**
     * 解析存在单位的文件大小，只匹配整数，支持KB,MB,GB，忽略大小写
     *
     * @param string 例如：1kb
     * @return 文件字节大小
     */
    public static long parseString(String string) {
        Matcher matcher = PATTERN.matcher(string);
        if (matcher.find()) {
            final long size = Long.parseLong(matcher.group(1));
            final String unit = Optional.ofNullable(matcher.group(2)).orElse("");
            return switch (unit.toUpperCase()) {
                case "KB" -> size * KB;
                case "MB" -> size * MB;
                case "GB" -> size * GB;
                default -> size;
            };
        }
        return 0L;
    }

}
