package com.example.chat.api;

import java.io.*;
import java.net.Socket;
import java.net.InetAddress;
import java.util.Map;
import java.util.stream.Collectors;

public class ServerAPI {
    private final String serverHost;
    private final int serverPort;
    private final Map<String, String> cache; // tham chiếu tới cache của PeerHandle

    public ServerAPI(String serverHost, int serverPort, Map<String, String> cache) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.cache = cache;
    }

    public boolean register(String name, int listenPort) {
        try (Socket socket = new Socket(serverHost, serverPort);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true)) {

            String ip = InetAddress.getLocalHost().getHostAddress();
            out.println("REGISTER " + name + " " + ip + " " + listenPort);
            String res = in.readLine();
            System.out.println(res);
            return res != null && res.startsWith("REGISTERED");
        } catch (IOException e) {
            System.out.println("[Warning] Server offline, skipping registration: " + e.getMessage());
            return false;
        }
    }

    public String getList() {
        try (Socket socket = new Socket(serverHost, serverPort);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true)) {

            out.println("LIST");
            String res = in.readLine();
            if (res != null && !res.isEmpty() && !res.equals("EMPTY")) {
                // server returns something like "PEERS Alice, Bob" or "Alice:1.2.3.4:5001,Bob:..."
                // we accept a format "PEERS name1, name2" (legacy) or "name:ip:port, name:ip:port"
                // try to parse name:ip:port entries first
                String[] entries = res.split(",");
                for (String entry : entries) {
                    String e = entry.trim();
                    if (e.contains(":")) {
                        // either "name:ip:port" or "ip:port" — we'll try to parse name:ip:port
                        String[] kv = e.split(":", 3);
                        if (kv.length == 3) {
                            cache.put(kv[0].trim(), kv[1].trim() + ":" + kv[2].trim());
                        }
                    } else {
                        // ignore simple name-only entries
                    }
                }
                System.out.println("[Server list updated] " + cache);
            }
            return res;
        } catch (IOException e) {
            System.out.println("[Warning] Server offline, skipping getList: " + e.getMessage());
            if (!cache.isEmpty()) {
                return cache.entrySet().stream()
                        .map(en -> en.getKey() + ":" + en.getValue())
                        .collect(Collectors.joining(", "));
            }
            return "";
        }
    }

    public String lookup(String name) {
        // try local cache first
        if (cache.containsKey(name)) return cache.get(name);
        try (Socket socket = new Socket(serverHost, serverPort);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true)) {

            out.println("LOOKUP " + name);
            String res = in.readLine();
            if (res != null && !res.equals("NOT FOUND")) {
                cache.put(name, res.trim());
            }
            return res;
        } catch (IOException e) {
            System.out.println("[Warning] Server offline, lookup failed: " + e.getMessage());
            return cache.get(name); // may be null
        }
    }

    // allow PeerHandle to add to cache (used by UDP discovery)
    public void cache(String name, String addr) {
        if (name == null || addr == null) return;
        cache.put(name, addr);
    }
}
