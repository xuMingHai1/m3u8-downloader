/*
 * SPDX-FileCopyrightText: 2024 xuMingHai <173535609@qq.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package xyz.xuminghai.m3u8_downloader.task;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import xyz.xuminghai.m3u8_downloader.config.CommonData;

import java.nio.file.Path;

/**
 * 2024/5/5 上午4:49 星期日<br/>
 *
 * @author xuMingHai
 */
public class M3U8Service extends Service<Path> {

    private M3U8 m3u8;

    private M3U8Task m3u8Task;

    private final BooleanProperty disablePause = new SimpleBooleanProperty();
    private final ObjectProperty<Throwable> retryableException = new SimpleObjectProperty<>();
    private final StringProperty downloadSpeed = new SimpleStringProperty();

    public M3U8Service() {
        super.setExecutor(CommonData.EXECUTOR);
        retryableException.addListener((_, _, newValue) -> {
            if (newValue != null) {
                retryableFailure(newValue);
            }
        });
    }

    @Override
    protected Task<Path> createTask() {
        m3u8Task = new M3U8Task(m3u8);
        disablePause.bind(m3u8Task.disablePauseProperty());
        retryableException.bind(m3u8Task.retryableExceptionProperty());
        downloadSpeed.bind(m3u8Task.downloadSpeedProperty());
        return m3u8Task;
    }

    public void start(M3U8 m3u8) {
        this.m3u8 = m3u8;
        start();
    }

    @Override
    public void reset() {
        super.reset();
        m3u8 = null;
        m3u8Task = null;
        disablePause.unbind();
        disablePause.set(false);
        retryableException.unbind();
        retryableException.set(null);
        downloadSpeed.unbind();
        downloadSpeed.set("");
    }

    public ReadOnlyBooleanProperty disablePauseProperty() {
        return disablePause;
    }

    public boolean pause() {
        if (Platform.isFxApplicationThread()
                && getState() == State.RUNNING) {
            return m3u8Task.pause();
        }
        return false;
    }

    public boolean resume(M3U8 m3u8) {
        if (Platform.isFxApplicationThread()
                && getState() == State.RUNNING) {
            if (m3u8Task.resume(m3u8)) {
                this.m3u8 = m3u8;
                return true;
            }
        }
        return false;
    }

    /**
     * 可以重试的失败
     *
     * @param e 失败异常
     */
    protected void retryableFailure(Throwable e) {

    }

    public boolean retry(M3U8 m3u8) {
        if (Platform.isFxApplicationThread()
                && getState() == State.RUNNING) {
            if (m3u8Task.retry(m3u8)) {
                this.m3u8 = m3u8;
                return true;
            }
        }
        return false;
    }

    public ReadOnlyStringProperty downloadSpeedProperty() {
        return downloadSpeed;
    }

}
