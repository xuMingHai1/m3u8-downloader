package xyz.xuminghai.m3u8_downloader.config;

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
     * 帮助地址
     */
    String HELP_URI = "https://bilibili.com";

    /**
     * APP使用的线程执行器
     */
    ExecutorService EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

}
