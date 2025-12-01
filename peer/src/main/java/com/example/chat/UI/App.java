package com.example.chat.UI;

import com.example.chat.core.PeerHandle;
import com.example.chat.db.ChatDatabase;
import com.example.chat.db.Message;
import com.example.chat.listener.MessageListener;
import javafx.application.Application;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;


import javafx.event.ActionEvent;

import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;

import java.util.List;

public class App extends Application {

    private Stage primaryStage;
    private PeerHandle peer;
    private ChatDatabase db;
    private String myName;

    private VBox messagesBox;
    private TextField targetField;
    private TextField msgField;
    private ScrollPane scrollPane;

    private String SERVER_IP_DEFAULT = "127.0.0.1";
    private int SERVER_PORT_DEFAULT = 5000;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        showLoginScreen();
    }

    private void showLoginScreen() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(15));
        root.setAlignment(Pos.CENTER);

        Label nameLabel = new Label("Enter your name:");
        TextField nameField = new TextField();
        Button loginBtn = new Button("Login");
        Label statusLabel = new Label();

        root.getChildren().addAll(nameLabel, nameField, loginBtn, statusLabel);

        loginBtn.setOnAction(e -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                statusLabel.setText("⚠️ Please enter your name");
                return;
            }

            try {
                peer = new PeerHandle(name, SERVER_IP_DEFAULT, SERVER_PORT_DEFAULT);
            } catch (IOException ex) {
                statusLabel.setText("❌ Failed to initialize peer: " + ex.getMessage());
                return;
            }

            myName = name;
            db = new ChatDatabase("./chat_history.db"); // local DB

            boolean registered = peer.register();
            if (registered) {
                showChatScreen();
            } else {
                statusLabel.setText("⚠️ Server offline, continuing in P2P mode...");
                showChatScreen();
            }

            // setup listener for incoming messages / files
            peer.setListener(new MessageListener() {
                @Override
                public void onMessage(String sender, String msg) {

                    System.out.println("[Content received]: "+msg);
                    Platform.runLater(() -> appendMessage(sender+": "+msg));
                }

                @Override
                public void onFileReceived(String senderName, String filename, String absPath, long size) {
                    Platform.runLater(() -> appendMessage(
                            String.format("<b>%s:</b> <a href='%s'>📂 %s</a> (%.2f KB)<br>",
                                    senderName, absPath, filename, size / 1024.0)
                    ));
                }
            });
        });

        Scene scene = new Scene(root, 350, 200);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Peer Login");
        primaryStage.show();
    }

    private void showChatScreen() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));


        messagesBox = new VBox(5);
        scrollPane = new ScrollPane(messagesBox);
        scrollPane.setFitToWidth(true);
        root.setCenter(scrollPane);

        // bottom panel
        HBox bottom = new HBox(5);
        targetField = new TextField();
        targetField.setPromptText("Receiver");
        targetField.setPrefWidth(100);

        msgField = new TextField();
        msgField.setPromptText("Message");
        msgField.setPrefWidth(300);
        msgField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) sendText();
        });

        Button sendBtn = new Button("Send");
        sendBtn.setOnAction(e -> sendText());

        Button fileBtn = new Button("Send File");
        fileBtn.setOnAction(e -> sendFile());

        Button loadHistoryBtn = new Button("Load History");
        loadHistoryBtn.setOnAction(this::loadHistory);

        bottom.getChildren().addAll(targetField, msgField, sendBtn, fileBtn, loadHistoryBtn);
        root.setBottom(bottom);

        Scene scene = new Scene(root, 700, 600);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Chat - " + myName);
        primaryStage.show();
    }

    private void appendMessage(String htmlMsg) {
        Label lbl = new Label(htmlMsg.replaceAll("<[^>]+>", ""));
        lbl.setWrapText(true);

        messagesBox.getChildren().add(lbl);

        // phải dùng runLater để ScrollPane cập nhật layout rồi mới cuộn
        Platform.runLater(() -> scrollPane.setVvalue(1.0));
    }


    private void sendText() {
        String target = targetField.getText().trim();
        String msg = msgField.getText().trim();
        if (target.isEmpty() || msg.isEmpty()) return;

        new Thread(() -> {
            try {
                String addr = peer.lookup(target);
                if ("NOT FOUND".equals(addr)) {
                    Platform.runLater(() -> appendMessage("[Error] Peer '" + target + "' not found"));
                    return;
                }
                peer.sendToByAddr(addr, msg);
                db.insertMessage(new Message(myName, target, msg, false, null));

                Platform.runLater(() -> appendMessage("<b>Me:</b> " + msg));
            } catch (Exception e) {
                Platform.runLater(() -> appendMessage("[Error] " + e.getMessage()));
            } finally {
                Platform.runLater(() -> msgField.clear());
            }
        }).start();
    }

    private void sendFile() {
        String target = targetField.getText().trim();
        if (target.isEmpty()) {
            appendMessage("[Error] Enter target name first!");
            return;
        }

        FileChooser chooser = new FileChooser();
        File file = chooser.showOpenDialog(primaryStage);
        if (file == null) return;


        new Thread(() -> {
            try {
                String addr = peer.lookup(target);
                if ("NOT FOUND".equals(addr)) {
                    Platform.runLater(() -> appendMessage("[Error] Peer '" + target + "' not found"));
                    return;
                }
                peer.sendFileByAddr(addr, file.getAbsolutePath());
                db.insertMessage(new Message(myName, target,file.getAbsolutePath(), true, file.getAbsolutePath()));
                Platform.runLater(() -> appendMessage("Me: " + file.getName()));

            } catch (Exception e) {
                Platform.runLater(() -> appendMessage("[Error] " + e.getMessage()));
            }
        }).start();
    }

    private void loadHistory(ActionEvent e) {
        String target = targetField.getText().trim();
        if (target.isEmpty()) {
            appendMessage("[Error] Enter target name first!");
            return;
        }

        new Thread(() -> {
            List<Message> msgs = db.loadConversationAsc(myName, target, 200);
            Platform.runLater(() -> {
                messagesBox.getChildren().clear();
                for (Message m : msgs) {
                    boolean isMe = m.getFromUser().equals(myName);
                    String senderLabel = isMe ? "Me" : m.getFromUser();

                    if (m.isFile()) {
                        Hyperlink link = new Hyperlink(m.getFilePath());
                        link.setOnAction(ev -> {
                            try {
                                java.awt.Desktop.getDesktop().open(new File(m.getFilePath()));
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                        });

                        HBox box = new HBox(5, new Label(senderLabel + ": "), link);
                        messagesBox.getChildren().add(box);

                    } else {
                        Label lbl = new Label(senderLabel + ": " + m.getContent());
                        messagesBox.getChildren().add(lbl);
                    }

                }
            });
        }).start();
    }
}
