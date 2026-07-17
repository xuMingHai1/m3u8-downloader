/*
 * SPDX-FileCopyrightText: 2024 xuMingHai <173535609@qq.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package xyz.xuminghai.m3u8_downloader.config.logback;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.Configurator;
import ch.qos.logback.core.spi.ContextAwareBase;
import org.slf4j.Logger;
import xyz.xuminghai.m3u8_downloader.config.CommonData;


/**
 * 2022/3/1 20:49 星期二<br/>
 * 基于SPI的logback配置
 *
 * @author xuMingHai
 */
public class LogbackConfig extends ContextAwareBase implements Configurator {

    private static final LoggerModel LOGGER_MODEL = LoggerModel.modelOf(System.getProperty("app.loggerModel"),
            CommonData.devModel ? LoggerModel.CONSOLE : LoggerModel.FILE);
    private static final Level LOGGER_LEVEL = Level.toLevel(System.getProperty("app.loggerLevel"),
            CommonData.devModel ? Level.DEBUG : Level.INFO);

    @Override
    public ExecutionStatus configure(LoggerContext loggerContext) {
        // 获取root记录器
        ch.qos.logback.classic.Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        // 日志级别
        rootLogger.setLevel(LOGGER_LEVEL);
        // 在root记录器添加附加器
        LOGGER_MODEL.createAppenders(loggerContext).forEach(rootLogger::addAppender);
        // 注册系统关闭挂钩
        Runtime.getRuntime().addShutdownHook(Thread.ofPlatform()
                .name("MyLogShutdownHook")
                .daemon()
                .inheritInheritableThreadLocals(false)
                .unstarted(loggerContext::stop));
        return ExecutionStatus.DO_NOT_INVOKE_NEXT_IF_ANY;
    }

}
