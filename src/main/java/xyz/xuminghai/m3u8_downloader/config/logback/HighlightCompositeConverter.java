package xyz.xuminghai.m3u8_downloader.config.logback;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.color.ANSIConstants;
import ch.qos.logback.core.pattern.color.ForegroundCompositeConverterBase;

/**
 * 2024/3/29 17:05 星期五<br/>
 * 自定义高亮颜色
 *
 * @author xuMingHai
 */
public class HighlightCompositeConverter extends ForegroundCompositeConverterBase<ILoggingEvent> {

    private static final String BOLD_RED_FG = ANSIConstants.BOLD + ANSIConstants.RED_FG;

    private static final String BOLD_YELLOW_FG = ANSIConstants.BOLD + ANSIConstants.YELLOW_FG;

    @Override
    protected String getForegroundColorCode(ILoggingEvent event) {
        Level level = event.getLevel();
        return switch (level.toInt()) {
            case Level.ERROR_INT -> BOLD_RED_FG;
            case Level.WARN_INT -> BOLD_YELLOW_FG;
            case Level.INFO_INT -> ANSIConstants.BLUE_FG;
            case Level.DEBUG_INT -> ANSIConstants.GREEN_FG;
            default -> ANSIConstants.DEFAULT_FG;
        };
    }
}
