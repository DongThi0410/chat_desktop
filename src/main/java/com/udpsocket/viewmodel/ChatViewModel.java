package com.udpsocket.viewmodel;

import com.udpsocket.model.Peer;
import com.udpsocket.model.PeerEventListener;
import com.udpsocket.model.PeerItem;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import com.udpsocket.helpers.MySqlHelper;
import com.udpsocket.helpers.MySqlHelper.ChatRow;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatViewModel implements PeerEventListener {

    private final Peer peer;

    private final StringProperty activePeer = new SimpleStringProperty(null);


    private final Map<String, ObservableList<String>> chatData = new HashMap<>();



    private final ObservableList<PeerItem> peersList = FXCollections.observableArrayList();

    private final ObservableList<String> peersOnline = FXCollections.observableArrayList();

    public ChatViewModel(Peer peer) {
        this.peer = peer;
        peer.setListener(this);
        List<String> partners = MySqlHelper.loadChatPartners(peer.getName());

        for (String name : partners) {
            boolean isOnline = MySqlHelper.loadPeerOnline(name).isPresent();
            peersList.add(new PeerItem(name, isOnline));
        }

    }

    public ObservableList<PeerItem> getPeersList() {
        return peersList;
    }
    public Map<String, ObservableList<String>> getChatData() {
        return chatData;
    }

    public ObservableList<String> getPeersOnline() {
        return peersOnline;
    }

    public ObservableList<String> getChatHistory(String peerName) {
        return chatData.computeIfAbsent(peerName, k -> FXCollections.observableArrayList());
    }

    public void setActivePeerAndLoadHistory(String peerName) {
        ObservableList<String> hist = getChatHistory(peerName);
        hist.clear();
        // load from DB
        List<ChatRow> rows = MySqlHelper.loadChat(peer.getName(), peerName, 2000);
        for (ChatRow r : rows) {
            String prefix = r.sender.equals(peer.getName()) ? "Me: " : (r.sender + ": ");
            hist.add(prefix + r.content);
        }
    }

    public void setActivePeer(String name) {
        Platform.runLater(() -> activePeer.set(name));
    }

    public String getActivePeer() {
        return activePeer.get();
    }


    public void sendMessage(String targetPeer, String msg) {
        if (targetPeer == null || msg == null || msg.trim().isEmpty()) return;
        peer.sendMessage(targetPeer, msg);
        MySqlHelper.saveMessage(peer.getName(), targetPeer, msg);

        getChatHistory(targetPeer).add("Me: " + msg);
    }

    @Override
    public void onMessageReceived(String from, String msg) {
        Platform.runLater(() -> {
            getChatHistory(from).add(from + ": " + msg);
        });
    }

    @Override
    public void setPeersOnline(List<String> peers){
        Platform.runLater(() -> {
            peersOnline.setAll(peers);
        });
    }

    @Override
    public void onPeerListUpdated(List<String> peers) {
        Platform.runLater(() -> {
            peersOnline.setAll(peers);
            peers.forEach(p -> chatData.putIfAbsent(p, FXCollections.observableArrayList()));
        });
    }

    @Override
    public void onPeerOnline(String name) {
        Platform.runLater(() -> {
            for (int i = 0; i < peersList.size(); i++) {
                PeerItem p = peersList.get(i);
                if (p.getUsername().equals(name)) {
                    peersList.set(i, new PeerItem(name, true));
                    return;
                }
            }

            peersList.add(new PeerItem(name, true));
        });
    }

    @Override
    public void onPeerOffline(String name) {
        Platform.runLater(() -> {
            for (int i = 0; i < peersList.size(); i++) {
                PeerItem p = peersList.get(i);
                if (p.getUsername().equals(name)) {
                    peersList.set(i, new PeerItem(name, false));
                    return;
                }
            }
        });
    }

}
