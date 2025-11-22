package com.udpsocket.model;
import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Peer {

    private final String name;

    private Socket tcpSocket;
    private DataInputStream dis;
    private DataOutputStream dos;

    private DatagramSocket udpSocket;
    private InetSocketAddress serverUdpAddr;

    private PeerEventListener listener;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private volatile boolean started = false;


    public Peer(String name, String serverIp, int tcpPort, int udpPort) throws IOException {
        this.name = name;

        tcpSocket = new Socket(serverIp, tcpPort);
        dis = new DataInputStream(tcpSocket.getInputStream());
        dos = new DataOutputStream(tcpSocket.getOutputStream());

        dos.writeUTF(name);
        dos.flush();

        udpSocket = new DatagramSocket();
        serverUdpAddr = new InetSocketAddress(serverIp, udpPort);

        dos.writeUTF("SET_UDP_PORT " + udpSocket.getLocalPort());
        dos.flush();


    }
    public Peer(String name, String serverIp, int tcpPort, int udpPort, PeerEventListener listener) throws IOException {
        this(name, serverIp, tcpPort, udpPort);
        setListener(listener);
        start();
    }
    public synchronized void start() {
        if (started) return;
        if (listener == null) {
            System.err.println("Warning: starting Peer without listener - some events may be missed");
        }
        started = true;
        executor.submit(this::tcpLoop);
        executor.submit(this::udpLoop);
    }
    public void setListener(PeerEventListener listener) {
        this.listener = listener;
    }

    public void sendMessage(String target, String msg) {
        try {
            dos.writeUTF("TEXT " + target + " " + msg);
            dos.flush();
        } catch (IOException e) {
            if (listener != null) listener.onError("sendMessage: " + e.getMessage());
        }
    }

    private void tcpLoop() {
        try {
            while (true) {
                String msg = dis.readUTF();

                String[] parts = msg.split(" ", 3);
                if (parts.length < 2) continue;

                String type = parts[0];
                String from = parts[1];
                String payload = parts.length > 2 ? parts[2] : "";

                switch (type.toUpperCase()) {

                    // Nhận tin nhắn
                    case "TEXT":
                        if (listener != null)
                            listener.onMessageReceived(from, payload);
                        break;

                    case "PEER_LIST":
                        if (listener != null) {
                            List<String> peers = payload.isEmpty()
                                    ? Collections.emptyList()
                                    : Arrays.asList(payload.split(","));
                            listener.onPeerListUpdated(peers);
                        }
                        break;

                    case "PEER_JOIN":
                        if (listener != null) listener.onPeerOnline(from);
                        break;

                    case "PEER_LEAVE":
                        if (listener != null) listener.onPeerOffline(from);
                        break;

                    default:
                        if (listener != null)
                            listener.onInfo("Unknown TCP msg: " + msg);
                }
            }

        } catch (IOException e) {
            if (listener != null)
                listener.onError("[TCP] " + e.getMessage());
        }
    }


    private void udpLoop() {
        byte[] buf = new byte[64 * 1024];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);

        try {
            while (true) {
                udpSocket.receive(packet);
                String s = new String(packet.getData(), 0, packet.getLength());

                if (!s.startsWith("MSG|")) continue;

                String[] parts = s.split("\\|", 3);
                if (parts.length < 3) continue;

                String sender = parts[1];
                String payload = parts[2];

                if (listener != null)
                    listener.onMessageReceived(sender, payload);
            }

        } catch (IOException e) {
            if (listener != null)
                listener.onError("[UDP] " + e.getMessage());
        }
    }


    public void close() {
        try {
            if (tcpSocket != null) tcpSocket.close();
            if (udpSocket != null) udpSocket.close();
        } catch (IOException ignored) {}

        executor.shutdownNow();
    }
    public String getName() { return name; }

}
