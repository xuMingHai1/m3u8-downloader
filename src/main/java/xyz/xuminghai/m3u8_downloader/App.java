package xyz.xuminghai.m3u8_downloader;

import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.application.HostServices;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.xuminghai.m3u8_downloader.config.CommonData;
import xyz.xuminghai.m3u8_downloader.control.ErrorAlert;
import xyz.xuminghai.m3u8_downloader.view.MainView;

import java.lang.reflect.InvocationTargetException;
import java.time.LocalTime;

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
        primaryStage.setTitle("M3U8下载器");
        primaryStage.getIcons().addAll(new Image("/img/app-icon.png"),
                new Image("/img/app-icon@2x.png"),
                new Image("/img/app-icon@3x.png"),
                new Image("/img/app-icon@4x.png"));
        primaryStage.setScene(createScene());
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

    private Scene createScene() {
        Scene scene = new Scene(new MainView(), 650.0, 450.0);
        // Control + Enter 重新加载
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.ENTER, KeyCombination.CONTROL_DOWN),
                () -> {
                    LOGGER.debug("重新加载时间 = {}", LocalTime.now());
                    try {
                        scene.setRoot(scene.getRoot().getClass().getConstructor().newInstance());
                    }
                    catch (NoSuchMethodException | InvocationTargetException | InstantiationException |
                           IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                });
        return scene;
    }
}
