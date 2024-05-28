package xyz.xuminghai.m3u8_downloader.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import xyz.xuminghai.m3u8_downloader.App;
import xyz.xuminghai.m3u8_downloader.config.CommonData;


/**
 * 2024/4/21 上午12:57 星期日<br/>
 *
 * @author xuMingHai
 */
public class MainView extends BorderPane {

    public MainView() {
        super.setId("main-view");
        super.getStylesheets().add("/css/main-view.css");
        leftView();
    }

    private void leftView() {
        // APP 标题
        final ImageView appIcon = new ImageView(CommonData.APP_ICON);
        appIcon.setFitWidth(32);
        appIcon.setFitHeight(32);
        final Text appTitle = new Text(CommonData.APP_TITLE);
        final HBox appTitleHBox = new HBox(appIcon, appTitle);
        appTitleHBox.setId("app-title-h-box");
        BorderPane.setMargin(appTitleHBox, new Insets(10, 10, 5, 5));

        // 切换按钮
        final VBox toggleButtonVBox = toggleButtonVBox();
        toggleButtonVBox.setId("toggle-button-v-box");
        BorderPane.setMargin(toggleButtonVBox, new Insets(20, 0, 0, 0));

        // 版本
        final Hyperlink versionLink = new Hyperlink(CommonData.VERSION);
        versionLink.setId("version-link");
        versionLink.setOnAction(_ -> App.hostServices.showDocument(CommonData.RELEASE_URI));
        BorderPane.setAlignment(versionLink, Pos.CENTER);
        BorderPane.setMargin(versionLink,
                new Insets(0, 0, 5, 0));

        // 左视图
        final BorderPane leftView = new BorderPane();
        leftView.setId("left-view");
        leftView.setTop(appTitleHBox);
        leftView.setCenter(toggleButtonVBox);
        leftView.setBottom(versionLink);
        super.setLeft(leftView);
    }


    private DownloadView downloadView;

    private DownloadView getDownloadView() {
        if (downloadView == null) {
            downloadView = new DownloadView();
        }
        return downloadView;
    }

    private AboutView aboutView;

    private AboutView getAboutView() {
        if (aboutView == null) {
            aboutView = new AboutView();
        }
        return aboutView;
    }

    private DonateView donateView;

    private DonateView getDonateView() {
        if (donateView == null) {
            donateView = new DonateView();
        }
        return donateView;
    }

    private VBox toggleButtonVBox() {
        // 切换按钮
        final ToggleGroup toggleGroup = new ToggleGroup();
        toggleGroup.selectedToggleProperty().addListener((_, oldValue, newValue) -> {
            if (newValue == null) {
                oldValue.setSelected(true);
            }
        });

        // m3u8 下载页面
        final ToggleButton m3u8DownloadToggle = new ToggleButton("M3U8下载");
        m3u8DownloadToggle.setToggleGroup(toggleGroup);
        m3u8DownloadToggle.setOnAction(_ -> super.setCenter(getDownloadView()));

        // 关于页面
        final ToggleButton aboutToggle = new ToggleButton("关于");
        aboutToggle.setToggleGroup(toggleGroup);
        aboutToggle.setOnAction(_ -> super.setCenter(getAboutView()));

        // 捐助页面
        final ToggleButton donateToggle = new ToggleButton("支持一下");
        donateToggle.setToggleGroup(toggleGroup);
        donateToggle.setOnAction(_ -> super.setCenter(getDonateView()));

        // 默认页面
        m3u8DownloadToggle.fire();
        return new VBox(m3u8DownloadToggle, aboutToggle, donateToggle);
    }


}
