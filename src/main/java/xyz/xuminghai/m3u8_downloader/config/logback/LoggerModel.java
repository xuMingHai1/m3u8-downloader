package xyz.xuminghai.m3u8_downloader.config.logback;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.layout.TTLLLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;

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
    FILE;


    static LoggerModel modelOf(String model) {
        final LoggerModel defaultModel = FILE;

        if (model == null) {
            return defaultModel;
        }

        if (model.equalsIgnoreCase(CONSOLE.name())) {
            return CONSOLE;
        }

        if (model.equalsIgnoreCase(FILE.name())) {
            return FILE;
        }

        return defaultModel;
    }

    Appender<ILoggingEvent> createAppender(LoggerContext loggerContext) {
        return createAppender(this, loggerContext);
    }

    private Appender<ILoggingEvent> createAppender(LoggerModel loggerModel, LoggerContext loggerContext) {
        return switch (loggerModel) {
            case CONSOLE -> createConsoleAppender(loggerContext);
            case FILE -> createFileAppender(loggerContext);
        };
    }

    private Appender<ILoggingEvent> createFileAppender(LoggerContext loggerContext) {
        // 固定格式布局（加载时间快）
        TTLLLayout ttllLayout = new TTLLLayout();
        ttllLayout.setContext(loggerContext);
        ttllLayout.start();

        // 文件附加器
        FileAppender<ILoggingEvent> fileAppender = new MyFileAppender();
        fileAppender.setContext(loggerContext);
        fileAppender.setEncoder(createEncoder(loggerContext, ttllLayout));
        fileAppender.start();

        return fileAppender;
    }

    private Appender<ILoggingEvent> createConsoleAppender(LoggerContext loggerContext) {
        // 模式布局
        PatternLayout patternLayout = new PatternLayout();
        patternLayout.setContext(loggerContext);
        // 自定义高亮转换
        patternLayout.getDefaultConverterMap().put("highlight", HighlightCompositeConverter.class.getName());
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
        // 激活配置
        layoutWrappingEncoder.start();

        return layoutWrappingEncoder;
    }

}


