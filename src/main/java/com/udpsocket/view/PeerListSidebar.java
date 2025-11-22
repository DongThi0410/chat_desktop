package com.udpsocket.view;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.layout.VBox;

import javafx.scene.control.ListView;

public class PeerListSidebar extends VBox {
    private final ListView<String> listView = new ListView<>();

    public interface PeerSelectedListener {
        void onPeerSelected(String peerName);
    }

    private PeerSelectedListener listener;

    public PeerListSidebar() {
        setPrefWidth(180);
        setStyle("-fx-background-color: #2b2b2b;");
        listView.setStyle("-fx-background-color: #3a3a3a; -fx-text-fill: white;");

        getChildren().add(listView);

        listView.setOnMouseClicked(e -> {
            String selected = listView.getSelectionModel().getSelectedItem();
            if (selected != null && listener != null) {
                listener.onPeerSelected(selected);
            }
        });
    }

    public void bindPeerList(ObservableList<String> peers) {
        listView.setItems(peers);

        peers.addListener((ListChangeListener<String>) c -> {
            System.out.println("SIDEBAR: PeerList changed → " + peers);
        });
    }



    public void setOnPeerSelected(PeerSelectedListener listener) {
        this.listener = listener;
    }
}
