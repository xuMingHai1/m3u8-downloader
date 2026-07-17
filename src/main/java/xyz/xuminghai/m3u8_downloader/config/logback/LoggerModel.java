/*
 * SPDX-FileCopyrightText: 2024 xuMingHai <173535609@qq.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package xyz.xuminghai.m3u8_downloader.config.logback;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy;
import ch.qos.logback.core.util.FileSize;
import xyz.xuminghai.m3u8_downloader.config.CommonData;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

/**
 * 2024/1/2 17:32 星期二<br/>
 * 日志模式
 *
 * @author xuMingHai
 */
public enum LoggerModel {

    /**
     * 控制台
     */
    CONSOLE,

    /**
     * 文件
     */
    FILE,

    /**
     * 控制台和文件
     */
    BOTH;

    private static final int MAX_HISTORY = 14;

    private static final FileSize MAX_FILE_SIZE = FileSize.valueOf("10MB");

    private static final FileSize TOTAL_SIZE_CAP = FileSize.valueOf("100MB");


    static LoggerModel modelOf(String model, LoggerModel defaultModel) {
        if (model == null) {
            return defaultModel;
        }

        for (LoggerModel loggerModel : values()) {
            if (model.equalsIgnoreCase(loggerModel.name())) {
                return loggerModel;
            }
        }

        return defaultModel;
    }

    List<Appender<ILoggingEvent>> createAppenders(LoggerContext loggerContext) {
        return switch (this) {
            case CONSOLE -> List.of(createConsoleAppender(loggerContext));
            case FILE -> List.of(createFileAppender(loggerContext));
            case BOTH -> List.of(createConsoleAppender(loggerContext), createFileAppender(loggerContext));
        };
    }

    private Appender<ILoggingEvent> createFileAppender(LoggerContext loggerContext) {
        createLogDirectory();

        // 模式布局
        PatternLayout patternLayout = new PatternLayout();
        patternLayout.setContext(loggerContext);
        patternLayout.setPattern("%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] --- %logger{36} : %msg%n%ex");
        patternLayout.start();

        // 文件附加器
        RollingFileAppender<ILoggingEvent> fileAppender = new RollingFileAppender<>();
        fileAppender.setContext(loggerContext);
        fileAppender.setName("file");
        fileAppender.setFile(CommonData.APP_LOG_DIR.resolve("app.log").toString());
        fileAppender.setAppend(true);
        fileAppender.setEncoder(createEncoder(loggerContext, patternLayout));

        // 滚动策略：保留14天，单文件10MB，总量100MB
        SizeAndTimeBasedRollingPolicy<ILoggingEvent> rollingPolicy = new SizeAndTimeBasedRollingPolicy<>();
        rollingPolicy.setContext(loggerContext);
        rollingPolicy.setParent(fileAppender);
        rollingPolicy.setFileNamePattern(CommonData.APP_LOG_DIR.resolve("app.%d{yyyy-MM-dd}.%i.log").toString());
        rollingPolicy.setMaxHistory(MAX_HISTORY);
        rollingPolicy.setMaxFileSize(MAX_FILE_SIZE);
        rollingPolicy.setTotalSizeCap(TOTAL_SIZE_CAP);
        rollingPolicy.start();
        fileAppender.setRollingPolicy(rollingPolicy);
        fileAppender.start();

        return fileAppender;
    }

    private Appender<ILoggingEvent> createConsoleAppender(LoggerContext loggerContext) {
        // 模式布局
        PatternLayout patternLayout = new PatternLayout();
        patternLayout.setContext(loggerContext);
        // 自定义高亮转换
        patternLayout.getDefaultConverterSupplierMap().put("highlight", HighlightCompositeConverter::new);
        patternLayout.setPattern("%d %highlight(%-5level) [%thread] --- %cyan(%logger{25}) : %msg %n");
        // 激活配置
        patternLayout.start();

        // 控制台附加器
        final ConsoleAppender<ILoggingEvent> consoleAppender = new ConsoleAppender<>();
        consoleAppender.setContext(loggerContext);
        // 设置附加器名字
        consoleAppender.setName("console");
        // 控制台附加器设置编码
        consoleAppender.setEncoder(createEncoder(loggerContext, patternLayout));
        // 激活此附加器
        consoleAppender.start();

        return consoleAppender;
    }

    private Encoder<ILoggingEvent> createEncoder(LoggerContext loggerContext, Layout<ILoggingEvent> layout) {
        LayoutWrappingEncoder<ILoggingEvent> layoutWrappingEncoder = new LayoutWrappingEncoder<>();
        layoutWrappingEncoder.setContext(loggerContext);
        layoutWrappingEncoder.setLayout(layout);
        layoutWrappingEncoder.setCharset(StandardCharsets.UTF_8);
        // 激活配置
        layoutWrappingEncoder.start();

        return layoutWrappingEncoder;
    }

    private void createLogDirectory() {
        try {
            Files.createDirectories(CommonData.APP_LOG_DIR);
        }
        catch (IOException e) {
            throw new UncheckedIOException("创建日志目录失败：" + CommonData.APP_LOG_DIR, e);
        }
    }

}

