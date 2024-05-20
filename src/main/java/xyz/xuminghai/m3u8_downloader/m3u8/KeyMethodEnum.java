package xyz.xuminghai.m3u8_downloader.m3u8;

/**
 * 2024/4/21 下午4:48 星期日<br/>
 * m3u8加密方式，定义的方法有：NONE、AES-128 和 SAMPLE-AES。
 *
 * @author xuMingHai
 */
public enum KeyMethodEnum {

    /**
     * NONE 的加密方法表示媒体段未
     * 加密。如果加密方法为 NONE，则
     * 不得存在其他属性。
     */
    NONE,

    /**
     * AES-128 加密方法表明媒体段已带有 128 位密钥的
     * 高级加密标准 (AES)
     * [ AES_128 ]、密码块链 (CBC) 和
     * 公钥加密标准 #7 (PKCS7)填充[ RFC5652 ]。
     * CBC 在每个段边界上重新启动，使用
     * 初始化向量 (iv) 属性值或媒体序列
     * 号作为 iv
     */
    AES_128,

    /**
     * SAMPLE-AES 加密方法意味着媒体段
     * 使用高级加密标准 [ AES_128 ]
     * 加密的媒体样本，例如音频或视频这些媒体
     * 流如何加密并封装在片段中取决于Pantos & May 信息 [第 15 页]
     * 媒体编码和片段的媒体格式。 fMP4 媒体
     * 加密 [ COMMON_ENC ]
     * 的“cbcs”方案进行加密。
     * Live Streaming (HLS) 示例加密规范中描述了
     * 包含 H.264 [ H_264 ]、AAC [ ISO_14496 ]、AC-3 [ AC_3 ]
     * 和增强型 AC-3 [ AC_3 ] 媒体流的其他媒体段格式的加密[样本编码]。
     */
    SAMPLE_AES;

    /**
     * 解析为method
     *
     * @param str 字符串
     * @return 默认为<span>{@link #NONE}</span>
     */
    public static KeyMethodEnum of(String str) {
        return switch (str) {
            case "AES-128" -> AES_128;
            case "SAMPLE-AES" -> SAMPLE_AES;
            case null, default -> NONE;
        };
    }
}
