package com.udpsocket.model;

import com.udpsocket.model.entity.FileMeta;
import javafx.application.Platform;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

public interface PeerEventListener {
    void onMessageReceived(String from, String msg);

    void setPeersOnline(List<String> peers);

    void onPeerListUpdated(List<String> peers);

    default void onFileMetaReceived(String from, String filename, String fileServerIp, int fileServerPort) {
    }

    default void onCallRequest(String from, String type) {
    }

    default void onCallEnd(String from) {
    }

    default void onAudioReceived(byte[] pcm) {
    }

    default void onVideoFrame(BufferedImage frame) {
    }

    default void onPeerOnline(String name) {
    }

    default void onPeerOffline(String name) {
    }

    default void onInfo(String info) {
    }

    default void onError(String err) {
    }

    default void onMessageReceivedRaw(String raw) {
    }
}