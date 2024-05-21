package xyz.xuminghai.m3u8_downloader;

import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.application.HostServices;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.xuminghai.m3u8_downloader.config.CommonData;
import xyz.xuminghai.m3u8_downloader.control.ErrorAlert;
import xyz.xuminghai.m3u8_downloader.view.MainView;

/**
 * 2024/4/18 上午3:05 星期四<br/>
 *
 * @author xuMingHai
 */
public class App extends Application {

    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

    /**
     * 启动时间
     */
    private static final long BOOT_TIME = System.currentTimeMillis();

    public static HostServices hostServices;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void init() {
        hostServices = super.getHostServices();
    }

    @Override
    public void start(Stage primaryStage) {
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        primaryStage.setTitle(CommonData.APP_TITLE);
        primaryStage.getIcons().addAll(CommonData.APP_ICON);
        primaryStage.setScene(new Scene(new MainView(), 750.0, 500.0));
        primaryStage.setResizable(false);
        // JavaFX线程设置错误提示
        Thread.currentThread().setUncaughtExceptionHandler((_, e) -> ErrorAlert.show(primaryStage, "未知的错误", e));
        // 显示窗体
        primaryStage.show();
        LOGGER.info("启动完成耗时 = {}ms", System.currentTimeMillis() - BOOT_TIME);
    }

    @Override
    public void stop() {
        CommonData.EXECUTOR.shutdownNow();
    }

}
