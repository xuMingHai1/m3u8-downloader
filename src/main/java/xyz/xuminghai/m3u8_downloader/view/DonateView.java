package xyz.xuminghai.m3u8_downloader.view;

import atlantafx.base.controls.Card;
import atlantafx.base.theme.Styles;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;

/**
 * 2024/5/27 下午4:51 星期一<br/>
 *
 * @author xuMingHai
 */
public class DonateView extends BorderPane {

    public DonateView() {
        super.setId("donate-view");
        super.getStylesheets().add("/css/donate-view.css");
        initTop();
        initCenter();
        initBottom();
    }

    private void initTop() {
        final Text titleText = new Text("支持一下");
        titleText.getStyleClass().add(Styles.TITLE_1);
        BorderPane.setAlignment(titleText, Pos.CENTER);
        BorderPane.setMargin(titleText, new Insets(30, 0, 0, 0));
        super.setTop(titleText);
    }

    private void initCenter() {
        final Card weChatCard = new Card();
        weChatCard.setHeader(new Text("微信"));
        weChatCard.setBody(qrCodeImageView("/img/WeChat.png"));

        final Card alipayCard = new Card();
        alipayCard.setHeader(new Text("支付宝"));
        alipayCard.setBody(qrCodeImageView("/img/Alipay.jpg"));


        final HBox qrCodeHBox = new HBox(weChatCard, alipayCard);
        qrCodeHBox.setId("qr-code-h-box");
        super.setCenter(qrCodeHBox);
    }

    private ImageView qrCodeImageView(String url) {
        final double fit = 240.0;
        final ImageView imageView = new ImageView(new Image(url, true));
        imageView.setFitWidth(fit);
        imageView.setFitHeight(fit);
        imageView.setPreserveRatio(true);
        return imageView;
    }

    private void initBottom() {
        final Text explexplanationText = new Text("为了项目能健康持续的发展, 我期望获得相应的资金支持, 你们的支持是我不断更新前进的动力!");
        explexplanationText.getStyleClass().addAll(Styles.TEXT_SMALL,
                Styles.TEXT_SUBTLE);
        BorderPane.setAlignment(explexplanationText, Pos.CENTER);
        BorderPane.setMargin(explexplanationText, new Insets(10, 0, 10, 0));
        super.setBottom(explexplanationText);
    }
}
