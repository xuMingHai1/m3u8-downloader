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

    private static final long KB = 1024L,
            MB = KB * KB,
            GB = KB * MB,
            TB = KB * GB;

    private static final BigDecimal KB_DECIMAL = new BigDecimal(KB),
            MB_DECIMAL = new BigDecimal(MB),
            GB_DECIMAL = new BigDecimal(GB),
            TB_DECIMAL = new BigDecimal(TB);

    public static String convertString(long size) {
        if (size < KB) {
            return size + "B";
        }

        final BigDecimal sizeDecimal = new BigDecimal(size);

        if (size < MB) {
            return quotient(sizeDecimal, KB_DECIMAL).concat("KB");
        }

        if (size < GB) {
            return quotient(sizeDecimal, MB_DECIMAL).concat("MB");
        }

        if (size < TB) {
            return quotient(sizeDecimal, GB_DECIMAL).concat("GB");
        }

        return quotient(sizeDecimal, TB_DECIMAL).concat("TB");
    }

    private static String quotient(BigDecimal sizeDecimal, BigDecimal divisor) {
        return sizeDecimal.divide(divisor, 2, RoundingMode.HALF_UP).toPlainString();
    }

    private static final Pattern PATTERN = Pattern.compile("(\\d+)(B|kB|KB|MB|GB|TB)?");

    /**
     * 解析计算机存储的文件大小
     *
     * @param string 例如：1KB
     * @return 文件字节大小
     */
    public static long parseString(String string) {
        Matcher matcher = PATTERN.matcher(string);
        if (matcher.find()) {
            final long size = Long.parseLong(matcher.group(1));
            final String unit = Optional.ofNullable(matcher.group(2)).orElse("");
            return switch (unit) {
                case "B", "" -> size;
                case "kB", "KB" -> size * KB;
                case "MB" -> size * MB;
                case "GB" -> size * GB;
                case "TB" -> size * TB;
                default -> throw new IllegalStateException("Unexpected value: " + unit);
            };
        }
        return 0L;
    }

}
