/*
 * SPDX-FileCopyrightText: 2024 xuMingHai <173535609@qq.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package xyz.xuminghai.m3u8_downloader.view;

import atlantafx.base.theme.Styles;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import xyz.xuminghai.m3u8_downloader.App;
import xyz.xuminghai.m3u8_downloader.config.CommonData;

/**
 * 2024/5/21 下午7:48 星期二<br/>
 *
 * @author xuMingHai
 */
public class AboutView extends VBox {

    public AboutView() {
        super.setId("about-view");
        super.getStylesheets().add("/css/about-view.css");
        super.getChildren().addAll(createHeader(), createBody(), createFooter());
    }

    private Node createHeader() {
        final ImageView appIcon = new ImageView(CommonData.APP_ICON);
        final Text appTitle = new Text(CommonData.APP_TITLE);
        appTitle.getStyleClass().add(Styles.TITLE_2);
        final HBox headerHBox = new HBox(appIcon, appTitle);
        headerHBox.setId("header-h-box");

        return headerHBox;
    }

    private Node createBody() {
        final GridPane aboutGridPane = new GridPane();
        aboutGridPane.setId("about-grid-pane");
        // rowIndex
        int rowIndex = 0;

        // APP Version
        {
            final Text appVersionTitle = new Text("APP Version");
            final Text appVersionValue = new Text(CommonData.VERSION);
            aboutGridPane.addRow(rowIndex++, appVersionTitle,
                    appVersionValue);
        }

        // APP Release Date
        {
            final Text appReleaseDateTitle = new Text("APP Release Date");
            final Text appReleaseDateValue = new Text(CommonData.RELEASE_DATE);
            aboutGridPane.addRow(rowIndex++, appReleaseDateTitle,
                    appReleaseDateValue);
        }

        // Java Version
        {
            final Text javaVersionTitle = new Text("Java Version");
            final Text javaVersionValue = new Text(System.getProperty("java.version"));
            aboutGridPane.addRow(rowIndex++, javaVersionTitle,
                    javaVersionValue);
        }

        // VM Name
        {
            final Text vmNameTitle = new Text("VM Name");
            final Text vmNameValue = new Text(System.getProperty("java.vm.name"));
            aboutGridPane.addRow(rowIndex++, vmNameTitle,
                    vmNameValue);
        }

        // OS Name
        {
            final Text osNameTitle = new Text("OS Name");
            final Text osNameValue = new Text(System.getProperty("os.name"));
            aboutGridPane.addRow(rowIndex++, osNameTitle,
                    osNameValue);
        }

        // JavaFX Version
        {
            final Text javaFXVersionTitle = new Text("JavaFX Version");
            final Text javaFXVersionValue = new Text(System.getProperty("javafx.version"));
            //noinspection UnusedAssignment
            aboutGridPane.addRow(rowIndex++, javaFXVersionTitle,
                    javaFXVersionValue);
        }

        VBox.setMargin(aboutGridPane, new Insets(75, 0, 50, 0));
        return aboutGridPane;
    }

    private Node createFooter() {
        final Hyperlink homeLink = new Hyperlink("xuMingHai1");
        homeLink.setId("home-link");
        homeLink.setOnAction(_ -> App.hostServices.showDocument(CommonData.HOME_URI));
        final HBox copyrightHBox = new HBox(new Text("版权所有 © 2024 "),
                homeLink,
                new Text("(173535609@qq.com)"));
        copyrightHBox.setId("copyright-h-box");

        return copyrightHBox;
    }

}
