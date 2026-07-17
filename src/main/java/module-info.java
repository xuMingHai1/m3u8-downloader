/*
 * SPDX-FileCopyrightText: 2024 xuMingHai <173535609@qq.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

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

    // logback 自定义配置实现类
    provides ch.qos.logback.classic.spi.Configurator with LogbackConfig;

    exports xyz.xuminghai.m3u8_downloader to javafx.graphics;

    opens css;
    opens img;
}
