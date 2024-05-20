package xyz.xuminghai.m3u8_downloader.control;

import atlantafx.base.theme.Styles;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.text.Text;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.xuminghai.m3u8_downloader.App;
import xyz.xuminghai.m3u8_downloader.config.CommonData;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * 2024/1/2 0:56 星期二<br/>
 * 错误窗口
 *
 * @author xuMingHai
 */
public class ErrorAlert extends Alert {

    private static final Logger LOGGER = LoggerFactory.getLogger(ErrorAlert.class);

    private static final ButtonType HELP_BUTTON_TYPE = new ButtonType("帮助", ButtonBar.ButtonData.HELP);

    public static void show(Window window, String headerText, Throwable throwable) {
        final ErrorAlert errorAlert = new ErrorAlert(headerText, throwable);
        errorAlert.initOwner(window);
        errorAlert.show();
    }

    public ErrorAlert(String headerText, Throwable throwable) {
        super(AlertType.ERROR, null, HELP_BUTTON_TYPE);

        // 设置help按钮行为
        Button helpButton = (Button) super.getDialogPane().lookupButton(HELP_BUTTON_TYPE);
        // 消耗事件，避免事件进一步传播
        helpButton.addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            App.hostServices.showDocument(CommonData.HELP_URI);
        });

        super.setHeaderText(headerText);

        if (throwable == null) {
            LOGGER.error(headerText);
        }
        else {
            // 输出日志
            LOGGER.error(headerText, throwable);
            super.setContentText(throwable.getMessage());
            fullStackTrace(throwable);
        }
    }


    private void fullStackTrace(Throwable throwable) {
        // 将堆栈信息打印到字符缓冲区
        StringWriter stringWriter = new StringWriter();
        throwable.printStackTrace(new PrintWriter(stringWriter));
        TextArea textArea = new TextArea(stringWriter.toString());
        textArea.setEditable(false);

        GridPane gridPane = new GridPane();
        Text title = new Text("Full StackTrace:");
        title.getStyleClass().add(Styles.TITLE_4);
        gridPane.addRow(0, title);
        gridPane.addRow(1, textArea);
        // 随容器扩大
        GridPane.setHgrow(textArea, Priority.ALWAYS);
        GridPane.setVgrow(textArea, Priority.ALWAYS);

        // 设置对话框扩展内容节点
        super.getDialogPane().setExpandableContent(gridPane);
    }
}
