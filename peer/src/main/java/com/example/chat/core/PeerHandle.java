package com.example.chat.core;


import com.example.chat.db.ChatDatabase;
import com.example.chat.db.Message;
import com.example.chat.listener.MessageListener;
import com.example.chat.api.ServerAPI;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PeerHandle {
    private final String name;
    private final ServerSocket serverSocket;
    private final int listenPort;

    // cache local (thread-safe)
    private final Map<String, String> cachedPeers = new ConcurrentHashMap<>();
    private ChatDatabase db;

    // modules
    private final MessageHandler messageHandler;
    private final ServerAPI serverAPI;

    private MessageListener listener;

    public PeerHandle(String name, String serverHost, int serverPort, ChatDatabase db) throws IOException {
        this.name = name;
this.db = db;
        // open server socket on ephemeral port
        this.serverSocket = new ServerSocket(0);
        this.listenPort = serverSocket.getLocalPort();

        // Server API (directory service) - will handle server offline silently
        this.serverAPI = new ServerAPI(serverHost, serverPort, cachedPeers);

        // Message handler (TCP accept/send)
        this.messageHandler = new MessageHandler(this.name, serverSocket, this::onIncomingMessage, this::onIncomingFile);


        // try fetching list from server (non-fatal)
        try {
            String list = serverAPI.getList();
            // serverAPI already updated cachedPeers when possible
            System.out.println("[Peer list after login] " + (list == null ? "" : list));
        } catch (Exception e) {
            System.out.println("[Warning] Cannot fetch peer list after login: " + e.getMessage());
        }
    }

    // --- public API for UI / caller ---
    public String getName() { return name; }
    public int getListenPort() { return listenPort; }

    public void setListener(MessageListener listener) {
        this.listener = listener;
        // Optionally push cached peers to UI here
    }

    public boolean register() {
        return serverAPI.register(name, listenPort);
    }

    public String getList() {
        return serverAPI.getList();
    }

    public String lookup(String peerName) {
        // check local cache first
        String cached = cachedPeers.get(peerName);
        if (cached != null) return cached;

        // otherwise ask server (non-fatal)
        String res = serverAPI.lookup(peerName);
        if (res != null && !res.equals("NOT FOUND")) {
            cachedPeers.put(peerName, res.trim());
            return res.trim();
        }
        return res;
    }

    public void sendToByName(String peerName, String message) {
        String addr = lookup(peerName);
        if (addr == null || addr.equals("NOT FOUND")) {
            System.out.println("[sendToByName] Peer not found: " + peerName);
            return;
        }
        messageHandler.sendText(addr, message);
    }

    public void sendToByAddr(String addr, String message) {
        messageHandler.sendText(addr, message);
    }

    public void sendFileByName(String peerName, String filePath) {
        String addr = lookup(peerName);
        if (addr == null || addr.equals("NOT FOUND")) {
            System.out.println("[sendFile] Peer not found: " + peerName);
            return;
        }
        try {
            messageHandler.sendFile(addr, filePath);
        } catch (Exception e) {
            System.err.println("[Error sending file] " + e.getMessage());
        }
    }
    public void sendFileByAddr(String addr, String filePath) {
        try {
            messageHandler.sendFile(addr, filePath);
        } catch (Exception e) {
            System.err.println("[Error sending file] " + e.getMessage());
        }
    }

    // --- callbacks from modules ---
    private void onDiscoveredPeer(String peerName, String addr) {
        // update local cache
        cachedPeers.put(peerName, addr);
        System.out.println("[Discovered peer] " + peerName + " -> " + addr);
    }

    private void onIncomingMessage(String sender, String message) {
        db.insertMessage(new Message(sender, this.name, message, false, null));

        if (listener != null) listener.onMessage(sender, message);
        else System.out.println("[Text received]"+message);
    }

    private void onIncomingFile(String sender, String filename, String absolutePath, long size) {
        db.insertMessage(new Message(sender, this.name,filename, true, absolutePath));

        if (listener != null) listener.onFileReceived(sender, filename, absolutePath, size);
        else System.out.println("[File received] from " + sender + ": " + filename + " -> " + absolutePath);
    }

    // call this to shutdown gracefully (close server socket, stop UDP)
    public void shutdown() {
        messageHandler.stop();
        try {
            serverSocket.close();
        } catch (IOException ignored) {}
    }
}