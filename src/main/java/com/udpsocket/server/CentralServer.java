package com.udpsocket.server;

import com.udpsocket.helpers.MySqlHelper;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
public class CentralServer {

    private final ServerSocket tcpServer;
    private final DatagramSocket udpSocket;

    private final ConcurrentHashMap<String, DataOutputStream> tcpClients = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, InetSocketAddress> udpClients = new ConcurrentHashMap<>();

    public CentralServer(int tcpPort, int udpPort) throws IOException {
        tcpServer = new ServerSocket(tcpPort);
        udpSocket = new DatagramSocket(udpPort);

        System.out.println("Server started TCP=" + tcpPort + " UDP=" + udpPort);

        new Thread(this::acceptTcpLoop).start();
        new Thread(this::udpRelayLoop).start();
    }

    private void acceptTcpLoop() {
        while (true) {
            try {
                Socket s = tcpServer.accept();
                new Thread(() -> handleClient(s)).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleClient(Socket s) {
        String name = "";
        try (DataInputStream dis = new DataInputStream(s.getInputStream());
             DataOutputStream dos = new DataOutputStream(s.getOutputStream())) {

            name = dis.readUTF();

            List<String> existingPeers = new ArrayList<>(tcpClients.keySet());

            tcpClients.put(name, dos);

            System.out.println("[JOIN] " + name + " from " + s.getInetAddress());

            sendPeerListTo(name);


            broadcastExcept(name, "PEER_JOIN " + name);

            while (true) {
                String msg = dis.readUTF();
                String[] parts = msg.split(" ", 3);
                String type = parts[0];

                switch (type) {
                    case "SET_UDP_PORT": {
                        int udpPort = Integer.parseInt(parts[1]);
                        udpClients.put(name,
                                new InetSocketAddress(s.getInetAddress(), udpPort));
                        MySqlHelper.upsertPeerOnline(name, s.getInetAddress().getHostAddress(), udpPort);

                        break;
                    }
                    case "TEXT": {
                        String target = parts[1];
                        String payload = parts.length > 2 ? parts[2] : "";
                        DataOutputStream t = tcpClients.get(target);
                        if (t != null) {
                            t.writeUTF("TEXT " + name + " " + payload);
                            t.flush();
                        }
                        break;
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("[LEAVE] disconnect");
            tcpClients.remove(name);
            udpClients.remove(name);
            MySqlHelper.removePeer(name);
            broadcastExcept(name, "PEER_LEAVE " + name);
        }
    }

    private void sendPeerListTo(String targetName) throws IOException {
        DataOutputStream dos = tcpClients.get(targetName);
        if (dos != null) {
            List<String> peers = new ArrayList<>(tcpClients.keySet());
            peers.remove(targetName);
            String list = String.join(",", peers);
            dos.writeUTF("PEER_LIST server " + list);
            dos.flush();
        }
    }


    // broadcast message except sender
    private void broadcastExcept(String excluded, String msg) {
        tcpClients.forEach((name, out) -> {
            if (!name.equals(excluded)) {
                try {
                    out.writeUTF(msg);
                    out.flush();
                } catch (Exception ignored) {
                }
            }
        });
    }

    // UDP relay
    private void udpRelayLoop() {
        byte[] buf = new byte[65536];
        DatagramPacket p = new DatagramPacket(buf, buf.length);

        while (true) {
            try {
                udpSocket.receive(p);

                udpClients.forEach((name, addr) -> {
                    if (!addr.getAddress().equals(p.getAddress())
                            || addr.getPort() != p.getPort()) {
                        try {
                            DatagramPacket out = new DatagramPacket(
                                    p.getData(), p.getLength(),
                                    addr.getAddress(), addr.getPort()
                            );
                            udpSocket.send(out);
                        } catch (IOException ignored) {
                        }
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public static void main(String[] args) throws IOException {
        new CentralServer(9000, 9001);
    }
}
