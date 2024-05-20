package xyz.xuminghai.m3u8_downloader.http;

/**
 * 2024/4/26 上午1:03 星期五<br/>
 * http内容压缩编码
 *
 * @author xuMingHai
 */
public enum ContentEncoding {

    /**
     * 没有压缩
     */
    IDENTITY,

    /**
     * gzip
     */
    GZIP,

    /**
     * deflate
     */
    DEFLATE;


    /**
     * 支持的内容压缩
     */
    public static final String CONTENT_ENCODING = "gzip, deflate";

    public static ContentEncoding of(String str) {
        if (str == null || str.isBlank()) {
            return IDENTITY;
        }
        return switch (str) {
            case "identity" -> IDENTITY;
            case "gzip" -> GZIP;
            case "deflate" -> DEFLATE;
            default -> throw new IllegalArgumentException("不支持的压缩编码");
        };
    }

}
