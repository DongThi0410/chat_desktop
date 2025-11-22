package com.udpsocket.helpers;

import javafx.scene.control.Alert;
import javafx.scene.control.Button;

public class UIHelper {
    public static void styleButton(Button b) {
        b.setStyle(
                "-fx-background-color: white;" +
                        "-fx-text-fill: black;" +
                        "-fx-border-color: black;" +
                        "-fx-border-width: 1;"
        );
    }

    public static void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg);
        a.setHeaderText(null);
        a.showAndWait();
    }

}
