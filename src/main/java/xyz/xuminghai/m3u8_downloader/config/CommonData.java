package xyz.xuminghai.m3u8_downloader.config;

import javafx.scene.image.Image;

import java.nio.file.Path;
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
     * 应用的工作目录
     */
    Path APP_DIR = Path.of(System.getProperty("user.dir"));

    /**
     * 默认下载目录
     */
    Path DOWNLOAD_DIR = Path.of(System.getProperty("user.home"), "Downloads");

    /**
     * APP使用的线程执行器
     */
    ExecutorService EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

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
    String VERSION = "v0.0.1";


    /**
     * 项目主页地址
     */
    String HOME_URI = "https://github.com/xuMingHai1";

    /**
     * 发布地址
     */
    String RELEASE_URI = HOME_URI + "/releases";

    /**
     * 备份发布地址
     */
    String BACKUP_RELEASE_URI = "";

    /**
     * 帮助地址
     */
    String HELP_URI = HOME_URI + "/issues";

}
