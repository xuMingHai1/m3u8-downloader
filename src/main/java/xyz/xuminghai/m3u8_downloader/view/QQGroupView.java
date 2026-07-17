/*
 * SPDX-FileCopyrightText: 2024 xuMingHai <173535609@qq.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package xyz.xuminghai.m3u8_downloader.view;

import atlantafx.base.controls.Card;
import atlantafx.base.theme.Styles;
import atlantafx.base.util.Animations;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.util.Duration;
import xyz.xuminghai.m3u8_downloader.config.CommonData;

/**
 * 2024/5/31 下午4:31 星期五<br/>
 *
 * @author xuMingHai
 */
public class QQGroupView extends BorderPane {

    private final Card qqGroupCard = new Card();
    private final Timeline timeline = Animations.zoomIn(qqGroupCard, Duration.millis(500.0));


    public QQGroupView() {
        qqGroupCardHeader();
        qqGroupCard.setBody(new ImageView(new Image("/img/qq_group_qrcode.png", true)));
        qqGroupCard.setMaxSize(300, 370.0);
        setCenter(qqGroupCard);
        super.sceneProperty().addListener((_, _, newValue) -> {
            if (newValue != null) {
                timeline.play();
            }
        });
    }

    private void qqGroupCardHeader() {
        final Circle qqGroupAvatar = new Circle(32.0, new ImagePattern(CommonData.APP_ICON));
        qqGroupAvatar.setStroke(Color.LIGHTGREY);
        final Text qqGroupName = new Text("M3U8下载器交流群");
        qqGroupName.getStyleClass().add(Styles.TITLE_3);
        final Text qqGroupNumber = new Text("群号：910349978");
        qqGroupNumber.getStyleClass().add(Styles.TEXT_SUBTLE);
        final HBox headerHBox = new HBox(30.0, qqGroupAvatar,
                new VBox(10.0, qqGroupName, qqGroupNumber));
        headerHBox.setAlignment(Pos.CENTER);

        qqGroupCard.setHeader(headerHBox);
    }

}
