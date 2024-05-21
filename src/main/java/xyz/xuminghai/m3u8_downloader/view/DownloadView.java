package xyz.xuminghai.m3u8_downloader.view;

import atlantafx.base.theme.Styles;
import atlantafx.base.util.Animations;
import javafx.animation.Timeline;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import xyz.xuminghai.m3u8_downloader.App;
import xyz.xuminghai.m3u8_downloader.config.CommonData;
import xyz.xuminghai.m3u8_downloader.control.ErrorAlert;
import xyz.xuminghai.m3u8_downloader.task.M3U8;
import xyz.xuminghai.m3u8_downloader.task.M3U8Service;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * 2024/5/21 下午2:19 星期二<br/>
 *
 * @author xuMingHai
 */
public class DownloadView extends BorderPane {
    /**
     * m3u8下载地址
     */
    private final TextArea uriTextArea = new TextArea();

    /**
     * http代理主机名
     */
    private final TextField httpProxyHostname = new TextField();

    /**
     * http代理端口
     */
    private final Spinner<Integer> httpProxyPort = new Spinner<>(0, 65535, 7890);

    /**
     * 自定义文件名
     */
    private final TextField fileNameTextField = new TextField();

    /**
     * 响应超时时间（秒）
     */
    private final Spinner<Integer> responseTimeout = new Spinner<>(1, 10, 3);

    /**
     * 下载目录
     */
    private final Hyperlink directoryLink = new Hyperlink(CommonData.DOWNLOAD_DIR.toString());

    /**
     * 更改下载目录按钮
     */
    private final Button chooseDirButton = new Button("选择目录");

    private final Button downloadButton = new Button("下载");
    private final Button pauseButton = new Button("暂停下载");
    private final Button continueButton = new Button("继续下载");
    private final Button cancelButton = new Button("取消下载");
    private final Button openFileButton = new Button("打开文件");
    private final Button resetButton = new Button("重置");
    private final Button retryButton = new Button("重试");

    private final HBox buttonHBox = new HBox();


    private final Text downloadMessage = new Text();
    /**
     * 进度条
     */
    private final ProgressBar downloadProgressBar = new ProgressBar(0);
    /**
     * 下载百分比
     */
    private final Text downloadPercentage = new Text();
    private final StackPane progressBarStackPane = new StackPane(downloadProgressBar, downloadPercentage);


    public DownloadView() {
        super.setId("download-view");
        super.getStylesheets().add("/css/download-view.css");
        super.sceneProperty().addListener((_, _, newValue) -> {
            if (newValue != null) {
                super.requestFocus();
            }
        });
        initView();
        initEvent();
    }

    private void initView() {
        initCenter();
        initBottom();
    }

    private void initCenter() {
        final GridPane gridPane = new GridPane();
        gridPane.setId("m3u8-grid-pane");

        {
            // m3u8地址
            uriTextArea.setId("uri-text-area");
            uriTextArea.setPromptText("例如：https://video.com/video/123/index.m3u8?ts=1234");
            GridPane.setColumnSpan(uriTextArea, 3);
            gridPane.addRow(0, new Text("M3U8地址"), uriTextArea);
        }


        {
            // http 代理
            httpProxyHostname.setId("http-proxy-hostname");
            httpProxyHostname.setPromptText("例如：127.0.0.1");
            httpProxyPort.setId("http-proxy-port");
            httpProxyPort.setEditable(true);
            GridPane.setColumnSpan(httpProxyHostname, 2);
            gridPane.addRow(1, new Text("http代理"),
                    httpProxyHostname, httpProxyPort);
            GridPane.setConstraints(httpProxyPort, 3, 1);
        }


        {
            // 自定义文件名、http响应超时
            fileNameTextField.setId("file-name-text-field");
            responseTimeout.setId("response-timeout");
            final Text timeoutText = new Text("响应超时时间（秒）");
            Tooltip.install(timeoutText, new Tooltip("在网络不好时可以适当调高"));
            GridPane.setHalignment(responseTimeout, HPos.RIGHT);
            gridPane.addRow(2, new Text("自定义文件名"), fileNameTextField,
                    timeoutText, responseTimeout);
        }


        {
            // 下载目录、选择下载目录
            directoryLink.setId("directory-link");
            GridPane.setColumnSpan(directoryLink, 2);
            GridPane.setHalignment(chooseDirButton, HPos.RIGHT);
            gridPane.addRow(3, new Text("下载目录"), directoryLink, chooseDirButton);
            GridPane.setConstraints(chooseDirButton, 3, 3);
        }


        {
            // 按钮
            buttonHBox.setId("button-h-box");
            downloadButton.setDefaultButton(true);
            pauseButton.setDefaultButton(true);
            continueButton.getStyleClass().add(Styles.SUCCESS);
            cancelButton.getStyleClass().add(Styles.DANGER);
            openFileButton.setDefaultButton(true);
            clearAndAddButton(downloadButton);
            GridPane.setColumnSpan(buttonHBox, 4);
            gridPane.addRow(4, buttonHBox);
        }

        super.setCenter(gridPane);
    }

    private void initBottom() {
        progressBarStackPane.setId("progress-bar-stack-pane");
        downloadMessage.getStyleClass().add(Styles.TEXT_SMALL);
        VBox.setMargin(downloadMessage, new Insets(1.0));
        downloadProgressBar.getStyleClass().add(Styles.LARGE);
        downloadProgressBar.setPrefWidth(Double.MAX_VALUE);
        downloadPercentage.getStyleClass().add(Styles.TEXT_NORMAL);
        downloadProgressBar.progressProperty().addListener(new ChangeListener<>() {

            private final DecimalFormat decimalFormat = new DecimalFormat("##.##");

            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                final double progress = newValue.doubleValue();
                if (progress == 0.0) {
                    downloadPercentage.setText("");
                    return;
                }
                downloadPercentage.setText(decimalFormat.format(progress * 100.0) + "%");
            }
        });

        super.setBottom(new VBox(downloadMessage, progressBarStackPane));
    }

    private void initEvent() {
        // 打开下载目录
        directoryLink.setOnAction(_ -> App.hostServices.showDocument(directoryLink.getText()));

        // 更改下载目录
        chooseDirButton.setOnAction(_ -> {
            final DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("选择下载目录");
            directoryChooser.setInitialDirectory(new File(directoryLink.getText()));
            final File file = directoryChooser.showDialog(super.getScene().getWindow());
            if (file != null) {
                directoryLink.setText(file.getAbsolutePath());
            }
        });
        downloadEvent();
    }

    private void clearAndAddButton(Button... buttons) {
        final ObservableList<Node> children = buttonHBox.getChildren();
        children.clear();
        children.addAll(buttons);
        super.requestFocus();
    }

    /**
     * m3u8下载任务
     */
    private final M3U8Service m3u8Service = new M3U8Service() {

        @Override
        protected void succeeded() {
            super.succeeded();
            // 修改进度条颜色
            progressBarStackPane.pseudoClassStateChanged(Styles.STATE_SUCCESS, true);
            // 打开文件按钮
            openFileButton.setOnAction(_ -> App.hostServices.showDocument(super.m3u8.filePath().toString()));
            clearAndAddButton(openFileButton, resetButton);
        }

        @Override
        protected void cancelled() {
            super.cancelled();
            reset();
            // 下载按钮
            clearAndAddButton(downloadButton);
        }

        @Override
        public void retryableFailure(Exception e) {
            super.retryableFailure(e);
            ErrorAlert.show(DownloadView.super.getScene().getWindow(),
                    "下载任务执行异常", e);
            // 继续和取消按钮
            clearAndAddButton(retryButton, cancelButton);
        }

        @Override
        protected void failed() {
            super.failed();
            // 修改进度条颜色
            progressBarStackPane.pseudoClassStateChanged(Styles.STATE_DANGER, true);
            ErrorAlert.show(DownloadView.super.getScene().getWindow(),
                    "下载任务执行异常", super.getException());
            // 设置重置按钮
            clearAndAddButton(resetButton);
        }

        @Override
        public void start() {
            super.start();
            // 绑定属性
            pauseButton.disableProperty().bind(super.disablePause());
            downloadMessage.textProperty().bind(super.messageProperty());
            downloadProgressBar.progressProperty().bind(super.progressProperty());
            // 设置暂停按钮
            clearAndAddButton(pauseButton);
            // 禁用可变参数
            uriTextArea.setDisable(true);
            httpProxyHostname.setDisable(true);
            httpProxyPort.setDisable(true);
            fileNameTextField.setDisable(true);
            responseTimeout.setDisable(true);
            chooseDirButton.setDisable(true);
        }

        @Override
        public void reset() {
            super.reset();
            // 修改进度条颜色
            progressBarStackPane.pseudoClassStateChanged(Styles.STATE_SUCCESS, false);
            progressBarStackPane.pseudoClassStateChanged(Styles.STATE_DANGER, false);
            // 解除绑定
            pauseButton.disableProperty().unbind();
            pauseButton.disableProperty().set(false);
            downloadMessage.textProperty().unbind();
            downloadMessage.setText("");
            downloadProgressBar.progressProperty().unbind();
            downloadProgressBar.setProgress(0);
            // 恢复可变参数
            uriTextArea.setDisable(false);
            httpProxyHostname.setDisable(false);
            httpProxyPort.setDisable(false);
            fileNameTextField.setDisable(false);
            responseTimeout.setDisable(false);
            chooseDirButton.setDisable(false);
        }
    };

    private Alert parameterInvalidAlert(Node node, String contentText) {
        final Alert alert = new Alert(Alert.AlertType.WARNING,
                contentText,
                ButtonType.CLOSE);
        alert.initOwner(super.getScene().getWindow());
        final Timeline timeline = Animations.shakeX(node);
        alert.setOnShown(_ -> {
            node.pseudoClassStateChanged(Styles.STATE_DANGER, true);
            timeline.play();
        });
        alert.setOnHidden(_ -> {
            node.pseudoClassStateChanged(Styles.STATE_DANGER, false);
            timeline.stop();
        });
        return alert;
    }

    private void uriTextAreaAlert(String contentText) {
        final Alert alert = parameterInvalidAlert(uriTextArea, contentText);
        alert.setHeaderText("M3U8地址");
        alert.show();
    }

    private void fileNameAlert(String contentText) {
        final Alert alert = parameterInvalidAlert(fileNameTextField, contentText);
        alert.setHeaderText("自定义文件名");
        alert.show();
    }

    private void downloadEvent() {
        // 下载按钮
        downloadButton.setOnAction(_ -> {
            // m3u8地址
            final String m3u8URI = Optional.ofNullable(uriTextArea.getText()).orElse("").strip();
            // 去除前后空白
            uriTextArea.setText(m3u8URI);
            // 参数校验
            if (m3u8URI.isBlank()) {
                uriTextAreaAlert("m3u8地址不能为空");
                return;
            }
            // 校验合法URL
            final URI uri;
            try {
                uri = URI.create(m3u8URI);
                final String scheme = uri.getScheme();
                // 不是http 或 https
                if (!"https".equals(scheme) && !"http".equals(scheme)) {
                    uriTextAreaAlert("m3u8地址不是http协议");
                    return;
                }
            }
            catch (IllegalArgumentException e) {
                uriTextAreaAlert("请输入正确的m3u8地址");
                return;
            }

            // http代理
            final String hostname = httpProxyHostname.getText();
            InetSocketAddress inetSocketAddress = null;
            if (!hostname.isEmpty()) {
                inetSocketAddress = new InetSocketAddress(hostname, httpProxyPort.getValue());
            }
            final ProxySelector proxySelector = ProxySelector.of(inetSocketAddress);

            // 文件路径
            final Path downloadDirPath = Path.of(directoryLink.getText());
            String fileNameString = fileNameTextField.getText();
            if (fileNameString == null || fileNameString.isBlank()) {
                fileNameString = String.valueOf(Instant.now().toEpochMilli());
            }
            final Path filePath;
            try {
                filePath = downloadDirPath.resolve(fileNameString + ".mp4");
            }
            // 包含无效字符
            catch (InvalidPathException e) {
                fileNameAlert("文件名包含无效字符");
                return;
            }

            // 文件是否已存在
            if (Files.exists(filePath)) {
                fileNameAlert(filePath.getFileName() + "已存在");
                return;
            }

            // 文件下载临时目录
            final Path downloadTempDirPath = downloadDirPath.resolve(fileNameString + "-temp");
            if (Files.exists(downloadTempDirPath)) {
                fileNameAlert("文件下载临时目录已存在");
                return;
            }

            final Duration timeout = Duration.ofSeconds(responseTimeout.getValue());
            m3u8Service.start(new M3U8(uri, proxySelector,
                    filePath, downloadTempDirPath, timeout));
        });

        // 暂停按钮事件
        pauseButton.setOnAction(_ -> {
            if (m3u8Service.pause()) {
                // 继续和取消按键
                clearAndAddButton(continueButton, cancelButton);
            }
        });

        // 继续按钮事件
        continueButton.setOnAction(_ -> {
            if (m3u8Service.resume()) {
                // 暂停按钮
                clearAndAddButton(pauseButton);
            }
        });

        // 取消按钮事件
        cancelButton.setOnAction(_ -> m3u8Service.cancel());

        // 重置按钮事件
        resetButton.setOnAction(_ -> {
            m3u8Service.reset();
            uriTextArea.setText(null);
            fileNameTextField.setText(null);
            clearAndAddButton(downloadButton);
        });

        // 重试按钮操作
        retryButton.setOnAction(_ -> {
            if (m3u8Service.retryable()) {
                clearAndAddButton(pauseButton);
            }
        });
    }

}
