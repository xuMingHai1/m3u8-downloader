import xyz.xuminghai.m3u8_downloader.config.logback.LogbackConfig;

module xyz.xuminghai.m3u8_downloader {
    // javafx 控件
    requires javafx.controls;
    // Java http 客户端
    requires java.net.http;
    // atlantafxUI
    requires atlantafx.base;
    // logback
    requires ch.qos.logback.classic;
    requires ch.qos.logback.core;
    requires org.slf4j;
    // lombok
    requires static lombok;
    // ffmpeg
    requires org.bytedeco.javacpp;
    requires org.bytedeco.ffmpeg;
    // jlink需要（特定平台）
//    requires org.bytedeco.ffmpeg.linux.x86_64;
//    requires org.bytedeco.ffmpeg.linux.arm64;
//    requires org.bytedeco.ffmpeg.macosx.arm64;
//    requires org.bytedeco.ffmpeg.macosx.x86_64;
    requires org.bytedeco.ffmpeg.windows.x86_64;

    // logback 自定义配置实现类
    provides ch.qos.logback.classic.spi.Configurator with LogbackConfig;

    exports xyz.xuminghai.m3u8_downloader to javafx.graphics;
    exports xyz.xuminghai.m3u8_downloader.config.logback to ch.qos.logback.core;

    opens css;
    opens img;
}