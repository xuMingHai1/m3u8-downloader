package xyz.xuminghai.m3u8_downloader.view;

import atlantafx.base.theme.Styles;
import javafx.scene.control.Hyperlink;
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
        initTop();
        initCenter();
        initBottom();
    }

    private void initTop() {
        final ImageView appIcon = new ImageView(CommonData.APP_ICON);
        final Text appTitle = new Text(CommonData.APP_TITLE);
        appTitle.getStyleClass().add(Styles.TITLE_2);
        final HBox topTitleHBox = new HBox(appIcon, appTitle);
        topTitleHBox.setId("top-title-h-box");
        super.setTop(topTitleHBox);
    }

    private void initCenter() {
        final GridPane aboutGridPane = new GridPane();
        aboutGridPane.setId("about-grid-pane");

        aboutGridPane.addRow(0, new Text("Java Version"),
                new Text(System.getProperty("java.version")));
        aboutGridPane.addRow(1, new Text("VM Name"),
                new Text(System.getProperty("java.vm.name")));
        aboutGridPane.addRow(2, new Text("OS Name"),
                new Text(System.getProperty("os.name")));
        aboutGridPane.addRow(3, new Text("JavaFX Version"),
                new Text(System.getProperty("javafx.version")));


        super.setCenter(aboutGridPane);
    }

    private void initBottom() {
        final Hyperlink homeLink = new Hyperlink("xuMingHai1");
        homeLink.setId("home-link");
        homeLink.setOnAction(_ -> App.hostServices.showDocument(CommonData.HOME_URI));
        final HBox copyrightHBox = new HBox(new Text("版权所有 © 2024-∞ "),
                homeLink,
                new Text("(173535609@qq.com)"));
        copyrightHBox.setId("copyright-h-box");
        super.setBottom(copyrightHBox);
    }

}
