/*
 * SPDX-FileCopyrightText: 2024 xuMingHai <173535609@qq.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package xyz.xuminghai.m3u8_downloader.config;

import javafx.scene.image.Image;

import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 2024/1/2 14:11 星期二<br/>
 * 常量数据
 *
 * @author xuMingHai
 */
public interface CommonData {

    /**
     * 应用名称
     */
    String APP_NAME = "m3u8-downloader";

    /**
     * 应用的工作目录
     */
    Path APP_DIR = Path.of(System.getProperty("user.dir"));

    /**
     * 应用数据目录
     */
    Path APP_DATA_DIR = defaultAppDataDir();

    /**
     * 应用日志目录
     */
    Path APP_LOG_DIR = APP_DATA_DIR.resolve("log");

    /**
     * 默认下载目录
     */
    Path DOWNLOAD_DIR = Path.of(System.getProperty("user.home"), "Downloads");

    /**
     * APP使用的线程执行器
     */
    ExecutorService EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * 开发者模式
     */
    boolean devModel = Boolean.TRUE.toString().equalsIgnoreCase(System.getProperty("app.devModel"));

    /**
     * 应用图标
     */
    Image APP_ICON = new Image("/img/app-icon.png");

    /**
     * 应用标题
     */
    String APP_TITLE = "M3U8下载器";

    /**
     * 应用版本
     */
    String VERSION = "v0.0.2";

    /**
     * 发布日期
     */
    String RELEASE_DATE = "2024-06-01";

    /**
     * 项目主页地址
     */
    String HOME_URI = "https://github.com/xuMingHai1/m3u8-downloader";

    /**
     * 发布地址
     */
    String RELEASE_URI = HOME_URI + "/releases";

    /**
     * 帮助地址
     */
    String HELP_URI = HOME_URI + "/issues";

    private static Path defaultAppDataDir() {
        final String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        final String userHome = System.getProperty("user.home", ".");

        if (osName.contains("win")) {
            final String localAppData = blankToNull(System.getenv("LOCALAPPDATA"));
            if (localAppData != null) {
                return Path.of(localAppData, APP_NAME);
            }
            final String appData = blankToNull(System.getenv("APPDATA"));
            if (appData != null) {
                return Path.of(appData, APP_NAME);
            }
            return Path.of(userHome, "AppData", "Local", APP_NAME);
        }

        if (osName.contains("mac")) {
            return Path.of(userHome, "Library", "Application Support", APP_NAME);
        }

        final String xdgDataHome = blankToNull(System.getenv("XDG_DATA_HOME"));
        if (xdgDataHome != null) {
            return Path.of(xdgDataHome, APP_NAME);
        }
        return Path.of(userHome, ".local", "share", APP_NAME);
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

}
