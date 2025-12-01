package com.example.chat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private static final Map<String, String> onlinePeers = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        ServerSocket socket = new ServerSocket(5000);
        System.out.println("server is running on port 5000");

        while (true) {
            Socket client = socket.accept();
            new Thread(new ClientHandle(client, onlinePeers)).start();
        }
    }
}
