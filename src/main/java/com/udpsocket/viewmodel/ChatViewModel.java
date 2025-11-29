package com.udpsocket.viewmodel;

import com.udpsocket.model.Peer;
import com.udpsocket.model.PeerEventListener;
import com.udpsocket.model.PeerItem;
import com.udpsocket.model.dto.ChatMessageDTO;
import com.udpsocket.model.entity.FileMeta;
import com.udpsocket.model.network.ServerAPI;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatViewModel implements PeerEventListener {

    public final Peer peer;
    public final ServerAPI serverAPI;

    private final StringProperty activePeer = new SimpleStringProperty(null);
    private final Map<String, ObservableList<String>> chatData = new HashMap<>();

    private final ObservableList<PeerItem> peersList = FXCollections.observableArrayList();
    private final ObservableList<String> peersOnline = FXCollections.observableArrayList();

    public ChatViewModel(Peer peer, String serverHost, int serverPort) {
        this.peer = peer;
        this.serverAPI = new ServerAPI(serverHost, serverPort);
        peer.setListener(this);

        // Load danh sách peer online từ server
        new Thread(() -> {
            List<String> names = serverAPI.getPeers(peer.getName());
            Platform.runLater(() -> {
                peersList.clear();
                for (String name : names) {
                    peersList.add(new PeerItem(name, true));
                }
            });
        }).start();

    }

    public ObservableList<PeerItem> getPeersList() {

        return peersList; }

    public ObservableList<String> getPeersOnline() { return peersOnline; }
    public Map<String, ObservableList<String>> getChatData() { return chatData; }

    public ObservableList<String> getChatHistory(String peerName) {
        return chatData.computeIfAbsent(peerName, k -> FXCollections.observableArrayList());
    }

    public void setActivePeer(String name) {
        Platform.runLater(() -> activePeer.set(name));
    }

    public String getActivePeer() { return activePeer.get(); }

    // Gửi tin nhắn
    public void sendMessage(String targetPeer, String msg) {
        if (targetPeer == null || msg == null || msg.trim().isEmpty()) return;

        // Gửi P2P trực tiếp
        peer.sendMessage(targetPeer, msg);

        // Gửi server lưu tin nhắn
        new Thread(() -> serverAPI.saveMessage(peer.getName(), targetPeer, msg)).start();

        // Cập nhật UI
        getChatHistory(targetPeer).add("Me: " + msg);
    }

    public void sendFile(String target, File file) throws IOException {
        peer.sendFileMessage(target, file, serverAPI);
    }

    // Load lịch sử chat từ server
    public void loadServerChatHistory(String peerName) {
        new Thread(() -> {
            List<ChatMessageDTO> history = serverAPI.getChatHistory(peer.getName(), peerName);
            ObservableList<String> hist = getChatHistory(peerName);
            Platform.runLater(() -> {
                hist.clear();
                for (ChatMessageDTO row : history) {
                    String prefix = row.getSender().equals(peer.getName()) ? "Me: " : (row.getSender() + ": ");
                    hist.add(prefix + row.getContent());
                }
            });
        }).start();
    }

    public void setPeersOnline(List<String> peers){
        Platform.runLater(() -> peersOnline.setAll(peers));
    }

    @Override
    public void onMessageReceived(String from, String msg) {
        Platform.runLater(() -> getChatHistory(from).add(from + ": " + msg));
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

    @Override
    public void onMessageReceivedRaw(String raw){
        if (raw.contains("\"action\":\"file_meta\"")) {
            FileMeta meta = FileMeta.fromJson(raw);
            Platform.runLater(()->{
                getChatHistory(meta.to).add(String.format("%s sent a file: %s (click to download)", meta.from, meta.filename));
            });
        }

    }
    public void logout(String name){
        new Thread(() -> serverAPI.logout(name)).start();
    }

}
