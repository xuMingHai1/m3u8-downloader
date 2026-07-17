/*
 * SPDX-FileCopyrightText: 2024 xuMingHai <173535609@qq.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package xyz.xuminghai.m3u8_downloader;

import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.application.HostServices;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class App extends Application {

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
        primaryStage.setScene(createScene());
        primaryStage.setResizable(false);
        primaryStage.setOnShown(_ -> openDevTools(primaryStage));

        // JavaFX线程设置错误提示
        Thread.currentThread().setUncaughtExceptionHandler((_, e) -> ErrorAlert.show(primaryStage, "未知的错误", e));
        // 显示窗体
        primaryStage.show();
        log.info("启动完成耗时 = {}ms", System.currentTimeMillis() - BOOT_TIME);
    }

    private void openDevTools(Stage primaryStage) {
        if (!CommonData.devModel) {
            return;
        }
        try {
            Class.forName("devtoolsfx.gui.GUI")
                    .getMethod("openToolStage", Stage.class, HostServices.class)
                    .invoke(null, primaryStage, getHostServices());
        }
        catch (ClassNotFoundException e) {
            log.warn("DevToolsFX is not available. Enable the dev profile to use developer tools.", e);
        }
        catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to open DevToolsFX.", e);
        }
    }

    @Override
    public void stop() {
        CommonData.EXECUTOR.shutdownNow();
    }

    private Scene createScene() {
        Scene scene = new Scene(new MainView(), 750.0, 500.0);
        // Control + Enter 重新加载
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.ENTER, KeyCombination.CONTROL_DOWN),
                () -> {
                    log.debug("重新加载时间 = {}", LocalTime.now());
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
