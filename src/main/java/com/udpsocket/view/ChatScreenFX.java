package com.udpsocket.view;

import com.udpsocket.model.Peer;
import com.udpsocket.model.PeerItem;
import com.udpsocket.viewmodel.ChatViewModel;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;


import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;


import javafx.application.Application;


public class ChatScreenFX extends Application {

    private final Peer peer;
    private final ChatViewModel viewModel;
    private static Peer staticPeer;

    private ListView<PeerItem> peersListView;
    private ListView<String> messageListView;
    private TextField inputField;

    private String currentChat = null;

    public ChatScreenFX(Peer peer, String serverHost, int serverPort) {
        this.peer = peer;
        this.viewModel = new ChatViewModel(peer, serverHost, serverPort);
    }
    public static void openWithPeer(Peer p, String serverHost, int serverPort) {
        staticPeer = p;
        ChatScreenFX screen = new ChatScreenFX(p, serverHost, serverPort);
        screen.launch();
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("P2P Chat - User: " + peerName());

        peersListView = new ListView<>();
        peersListView.setItems(viewModel.getPeersList());

        messageListView = new ListView<>();
        messageListView.setFocusTraversable(false);

        peersListView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV == null) return;

            currentChat = newV.getUsername();
            viewModel.setActivePeer(currentChat);

            ObservableList<String> displayList = viewModel.getChatHistory(currentChat);
            displayList.clear();
            messageListView.setItems(displayList);

// Load lịch sử từ server
            viewModel.loadServerChatHistory(currentChat);


            // 7. Auto scroll xuống cuối
            if (!displayList.isEmpty()) {
                Platform.runLater(() -> {
                    messageListView.scrollTo(displayList.size() - 1);
                });
            }
        });

        peersListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(PeerItem item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                // Chấm tròn
                Circle dot = new Circle(6);
                dot.setFill(item.isOnline() ? Color.LIMEGREEN : Color.GRAY);

                HBox box = new HBox(10, dot, new Label(item.getUsername()));
                setGraphic(box);
            }
        });

        inputField = new TextField();
        inputField.setPromptText("Type message...");

        Button sendBtn = new Button("Send");
        sendBtn.setOnAction(e -> sendCurrentMessage());

        Button logoutBtn = new Button("Logout");
        logoutBtn.setOnAction(e -> handleLogout(stage, peer.getName()));


        HBox bottom = new HBox(10, inputField, sendBtn, logoutBtn);
        bottom.setPadding(new Insets(10));

        BorderPane root = new BorderPane();
        root.setLeft(peersListView);
        root.setCenter(messageListView);
        root.setBottom(bottom);

        BorderPane.setMargin(peersListView, new Insets(10));
        peersListView.setPrefWidth(150);

        stage.setScene(new Scene(root, 650, 450));
        stage.show();
    }

    private void sendCurrentMessage() {
        String msg = inputField.getText().trim();
        if (msg.isEmpty()) return;
        if (currentChat == null) {
            new Alert(Alert.AlertType.WARNING, "Please select a peer to chat with.").show();
            return;
        }
        viewModel.sendMessage(currentChat, msg);
        inputField.clear();

        Platform.runLater(() -> {
            messageListView.scrollTo(messageListView.getItems().size() - 1);
        });
    }

    private void handleLogout(Stage stage, String name){
        try {
            viewModel.logout(peer.getName());
            peer.close();
            LoginScreenFX login = new LoginScreenFX();
            login.start(new Stage());
            stage.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private String peerName() {
        return "Peer";
    }
}
