package com.udpsocket.view;

import com.udpsocket.model.Peer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

public class LoginScreenFX extends Application {

    private TextField nameField;
    private TextField serverIpField;

    private static final int DEFAULT_TCP = 9000;
    private static final int DEFAULT_UDP = 9001;

    @Override
    public void start(Stage stage) {
        stage.setTitle("Login");

        Label nameLabel = new Label("Name:");
        nameField = new TextField();

        Label serverIpLabel = new Label("Server IP:");
        serverIpField = new TextField("127.0.0.1");

        Button loginBtn = new Button("Login");

        GridPane grid = new GridPane();
        grid.setVgap(8);
        grid.setHgap(8);
        grid.setPadding(new Insets(10));

        grid.addRow(0, nameLabel, nameField);
        grid.addRow(1, serverIpLabel, serverIpField);
        grid.add(loginBtn, 1, 3);

        loginBtn.setOnAction(e -> handleLogin(stage));

        stage.setScene(new Scene(grid, 400, 200));
        stage.show();
    }

    private void handleLogin(Stage stage) {
        String name = nameField.getText().trim();
        String serverIp = serverIpField.getText().trim();

        if (name.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Enter name").show();
            return;
        }

        try {
            // Kết nối tới server
            Peer peer = new Peer(name, serverIp, DEFAULT_TCP, DEFAULT_UDP);

            ChatScreenFX chat = new ChatScreenFX(peer, serverIp, DEFAULT_TCP);
            chat.start(new Stage());

            peer.start();
            stage.close();

        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "Cannot connect: " + ex.getMessage()).show();
        }
    }

    public static void main(String[] args) {
        launch();
    }
}

