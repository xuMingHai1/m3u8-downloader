package xyz.xuminghai.m3u8_downloader.task;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import xyz.xuminghai.m3u8_downloader.config.CommonData;

/**
 * 2024/5/5 上午4:49 星期日<br/>
 *
 * @author xuMingHai
 */
public class M3U8Service extends Service<Void> {

    protected M3U8 m3u8;

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
    protected Task<Void> createTask() {
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
        m3u8Task.close();
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

    public boolean resume() {
        if (Platform.isFxApplicationThread()
                && getState() == State.RUNNING) {
            return m3u8Task.resume();
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

    public boolean retry() {
        return m3u8Task.retry();
    }

    public ReadOnlyStringProperty downloadSpeedProperty() {
        return downloadSpeed;
    }

}
