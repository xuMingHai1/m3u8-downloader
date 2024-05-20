package xyz.xuminghai.m3u8_downloader.m3u8;

import lombok.Data;

import java.net.URI;

/**
 * 2024/4/21 下午5:48 星期日<br/>
 *
 * @author xuMingHai
 */
@Data
public class M3U8Key {

    /**
     * 加密方法
     */
    private final KeyMethodEnum method;

    /**
     * 密钥地址
     */
    private final URI uri;

    /**
     * 密钥数据
     */
    private byte[] key;

    /**
     * 初始向量
     */
    private final byte[] iv;

    public static M3U8Key NONE = new M3U8Key(KeyMethodEnum.NONE, null, null);

    public M3U8Key(KeyMethodEnum method, URI uri, byte[] iv) {
        this.method = method;
        this.uri = uri;
        this.iv = iv;
    }
}
