package xyz.xuminghai.m3u8_downloader.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

/**
 * 2024/5/25 下午1:38 星期六<br/>
 *
 * @author xuMingHai
 */
public final class BitstreamUtils {

    private BitstreamUtils() {
    }

    private static final BigInteger BYTE_BIT = BigInteger.valueOf(8L);

    private static final BigInteger Kb_INTEGER = BigInteger.valueOf(1000L),
            Mb_INTEGER = Kb_INTEGER.multiply(Kb_INTEGER),
            Gb_INTEGER = Kb_INTEGER.multiply(Mb_INTEGER),
            Tb_INTEGER = Kb_INTEGER.multiply(Gb_INTEGER),
            Pb_INTEGER = Kb_INTEGER.multiply(Tb_INTEGER);


    private static final BigDecimal Kb_DECIMAL = new BigDecimal(Kb_INTEGER),
            Mb_DECIMAL = new BigDecimal(Mb_INTEGER),
            Gb_DECIMAL = new BigDecimal(Gb_INTEGER),
            Tb_DECIMAL = new BigDecimal(Tb_INTEGER),
            Pb_DECIMAL = new BigDecimal(Pb_INTEGER);

    /**
     * 将文件字节大小转为比特流字符串
     *
     * @param fileSize 字节大小
     * @return 比特流字符串
     */
    public static String fileSizeConvertBitstreamString(long fileSize) {
        // 文件大小的比特数量
        final BigInteger bitBigInteger = BigInteger.valueOf(fileSize).multiply(BYTE_BIT);

        if (bitBigInteger.compareTo(Kb_INTEGER) < 0) {
            return bitBigInteger.toString().concat("b");
        }

        final BigDecimal bitBigDecimal = new BigDecimal(bitBigInteger);

        if (bitBigInteger.compareTo(Mb_INTEGER) < 0) {
            return quotient(bitBigDecimal, Kb_DECIMAL).concat("Kb");
        }

        if (bitBigInteger.compareTo(Gb_INTEGER) < 0) {
            return quotient(bitBigDecimal, Mb_DECIMAL).concat("Mb");
        }

        if (bitBigInteger.compareTo(Tb_INTEGER) < 0) {
            return quotient(bitBigDecimal, Gb_DECIMAL).concat("Gb");
        }

        if (bitBigInteger.compareTo(Pb_INTEGER) < 0) {
            return quotient(bitBigDecimal, Tb_DECIMAL).concat("Tb");
        }

        return quotient(bitBigDecimal, Pb_DECIMAL).concat("Pb");
    }

    private static String quotient(BigDecimal sizeDecimal, BigDecimal divisor) {
        return sizeDecimal.divide(divisor, 2, RoundingMode.HALF_UP).toPlainString();
    }


}
