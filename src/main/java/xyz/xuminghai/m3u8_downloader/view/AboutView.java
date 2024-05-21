package xyz.xuminghai.m3u8_downloader.view;

import atlantafx.base.theme.Styles;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Hyperlink;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import xyz.xuminghai.m3u8_downloader.App;
import xyz.xuminghai.m3u8_downloader.config.CommonData;

/**
 * 2024/5/21 下午7:48 星期二<br/>
 *
 * @author xuMingHai
 */
public class AboutView extends BorderPane {

    public AboutView() {
        super.setId("about-view");
        super.getStylesheets().add("/css/about-view.css");
        initCenter();
        initBottom();
    }

    private void initCenter() {
        final GridPane aboutGridPane = new GridPane();
        aboutGridPane.setId("about-grid-pane");

        final ImageView appIcon = new ImageView(CommonData.APP_ICON);
        final Text appTitle = new Text(CommonData.APP_TITLE);
        appTitle.getStyleClass().add(Styles.TITLE_2);
        aboutGridPane.addRow(0, appIcon, appTitle);

        aboutGridPane.addRow(1, new Text("Java Version"),
                new Text(System.getProperty("java.version")));
        aboutGridPane.addRow(2, new Text("VM Name"),
                new Text(System.getProperty("java.vm.name")));
        aboutGridPane.addRow(3, new Text("OS Name"),
                new Text(System.getProperty("os.name")));

        final Hyperlink homeLink = new Hyperlink("xuMingHai1");
        homeLink.setId("home-link");
        homeLink.setOnAction(_ -> App.hostServices.showDocument(CommonData.HOME_URI));
        final HBox copyrightHBox = new HBox(new Text("版权所有 © 2024-∞ "),
                homeLink,
                new Text("(173535609@qq.com)"));
        copyrightHBox.setId("copyright-h-box");
        GridPane.setColumnSpan(copyrightHBox, 2);
        aboutGridPane.addRow(4, copyrightHBox);

        super.setCenter(aboutGridPane);
    }

    private void initBottom() {
        final Text titleText = new Text("支持一下");
        titleText.getStyleClass().add(Styles.TITLE_3);
        BorderPane.setAlignment(titleText, Pos.CENTER);

        final HBox qrCodeHBox = new HBox(qrCodeImageView("/img/WeChat.png"),
                qrCodeImageView("/img/Alipay.jpg"));
        qrCodeHBox.setId("qr-code-h-box");
        BorderPane.setMargin(qrCodeHBox, new Insets(10, 0, 10, 0));

        final Text explexplanationText = new Text("为了项目能健康持续的发展, 我期望获得相应的资金支持, 你们的支持是我不断更新前进的动力!");
        explexplanationText.getStyleClass().addAll(Styles.TEXT_SMALL,
                Styles.TEXT_SUBTLE);
        BorderPane.setAlignment(explexplanationText, Pos.CENTER);
        BorderPane.setMargin(explexplanationText, new Insets(10, 0, 10, 0));

        final BorderPane donateBorderPane = new BorderPane();
        donateBorderPane.setTop(titleText);
        donateBorderPane.setCenter(qrCodeHBox);
        donateBorderPane.setBottom(explexplanationText);
        super.setBottom(donateBorderPane);
    }

    private ImageView qrCodeImageView(String url) {
        final double fit = 200.0;
        final ImageView imageView = new ImageView(new Image(url, true));
        imageView.setFitWidth(fit);
        imageView.setFitHeight(fit);
        imageView.setPreserveRatio(true);
        return imageView;
    }


}
