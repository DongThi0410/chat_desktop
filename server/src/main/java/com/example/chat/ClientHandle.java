package com.example.chat;
import java.io.*;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientHandle implements Runnable {
    private final Socket socket;
    private final Map<String, String> onlinePeers;

    public ClientHandle(Socket socket, Map<String, String> onlinePeers) {
        this.socket = socket;
        this.onlinePeers = onlinePeers;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true)
        ) {
            String line;
            while ((line = in.readLine()) != null) {

                if (line.startsWith("REGISTER")) {
                    String[] p = line.split(" ", 4);
                    if (p.length >= 4) {
                        onlinePeers.put(p[1], p[2] + ":" + p[3]);
                        out.println("REGISTERED " + p[1]);
                    }

                } else if (line.startsWith("LOOKUP")) {
                    String[] p = line.split(" ", 2);
                    if (p.length >= 2) {
                        String addr = onlinePeers.get(p[1]);
                        out.println(addr != null ? addr : "NOT FOUND");
                    }

                } else if (line.equals("LIST")) {
                    out.println(onlinePeers.isEmpty()
                            ? "EMPTY"
                            : "PEERS " + String.join(", ", onlinePeers.keySet()));

                } else if (line.startsWith("UNREGISTER")) {
                    String[] p = line.split(" ", 2);
                    if (p.length >= 2) {
                        onlinePeers.remove(p[1]);
                        out.println("UNREGISTER " + p[1]);
                    }

                } else {
                    out.println("NO CMD");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {}
        }
    }
}
