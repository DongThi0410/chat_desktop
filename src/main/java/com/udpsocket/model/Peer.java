package com.udpsocket.model;
import com.udpsocket.model.network.ServerAPI;
import javafx.application.Platform;

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

    public void sendMessage(String target, String msg) {
        try {
            dos.writeUTF("TEXT " + target + " " + msg);
            dos.flush();
        } catch (IOException e) {
            if (listener != null) listener.onError("sendMessage: " + e.getMessage());
        }
    }

    public void sendFileToPeer(String targetHost, int targetPort, File file) throws IOException {
        executor.submit(()-> {
            try (Socket s = new Socket(targetHost, targetPort);
            FileInputStream fis = new FileInputStream(file);
            OutputStream os = s.getOutputStream();){
                byte[] buf = new byte[8192];
                int read;
                while ((read = fis.read(buf)) != -1) {
                    os.write(buf, 0, read);
                }
                os.flush();
                if (listener!=null){
                    listener.onInfo("File sent to " + targetHost + ":" + targetPort + " -> " + file.getName());
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void receiveFile(int listenPort, File saveTo){
        executor.submit(()-> {
            try (ServerSocket ss = new ServerSocket(listenPort);
            Socket s = ss.accept();
            InputStream in = s.getInputStream();
            FileOutputStream fos = new FileOutputStream(saveTo);){
                byte[] buf = new byte[8192];
                int read;
                while ((read = in.read(buf)) != -1) {
                    fos.write(buf, 0, read);
                }
                fos.flush();

                if (listener!=null){
                    listener.onInfo("File sent to " + saveTo.getName());
                    Platform.runLater(() -> listener.onMessageReceivedRaw(String.format("{\"action\":\"file_meta\",\"from\":\"%s\",\"to\":\"%s\",\"filename\":\"%s\"}",
                            "peer", name, saveTo.getName())));
                }

            } catch (IOException e) {
                if (listener != null) listener.onError("receiveFile: " + e.getMessage());
            }
        });
    }

    public void sendFileMessage(String target, File file, ServerAPI serverAPI) {
        if (!file.exists()) return;

        new Thread(() -> {
            try {
                String fileUrl = serverAPI.uploadFile(file, name, target);

                String metaMsg = String.format("{\"action\":\"file_meta\",\"from\":\"%s\",\"to\":\"%s\",\"filename\":\"%s\",\"size\":%d,\"url\":\"%s\"}",
                        name, target, file.getName(), file.length(), fileUrl);

                dos.writeUTF(metaMsg);
                dos.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }


    public void close() {
        try {
            if (tcpSocket != null) tcpSocket.close();
            if (udpSocket != null) udpSocket.close();
        } catch (IOException ignored) {}

        executor.shutdownNow();
    }

    public void uploadFileToServer(String filePath, String serverIp, int serverPort, String target) throws IOException {
        File f = new File(filePath);
        if (!f.exists()) throw new FileNotFoundException(filePath);

        String url = "http://" + serverIp + ":" + serverPort + "/upload?name=" + URLEncoder.encode(f.getName(), "UTF-8")
                + "&from=" + URLEncoder.encode(this.name, "UTF-8")
                + "&to=" + URLEncoder.encode(target, "UTF-8");

        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setFixedLengthStreamingMode((int) f.length());
        conn.setRequestProperty("Content-Type", "application/octet-stream");
        conn.connect();

        try (OutputStream out = conn.getOutputStream();
             FileInputStream fis = new FileInputStream(f)) {
            byte[] buf = new byte[8192];
            int r;
            while ((r = fis.read(buf)) != -1) out.write(buf,0,r);
            out.flush();
        }

        int code = conn.getResponseCode();
        if (code != 200) throw new IOException("Upload failed code:" + code);

        try (InputStream in = conn.getInputStream();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()
        ){

            byte[] buf = new byte[8192];
            int r;
            while ((r=in.read(buf)) != -1) baos.write(buf,0,r);
            String json = baos.toString("UTF-8");
            String id = extractJson(json, "id");

            String sizeS = extractJson(json, "size");

            long size = sizeS!=null?Long.parseLong(sizeS):f.length();

            dos.writeUTF("FILE_META " + target + " " + id + " " + f.getName() + " " + serverIp + " " + serverPort + " " + size);
            dos.flush();
        }


    }

    private static String extractJson(String json, String key) {
        String pat = "\"" + key + "\":";
        int i = json.indexOf(pat);
        if (i<0) return null;
        int j = json.indexOf(',', i+pat.length());
        int k = json.indexOf('}', i+pat.length());
        int end = j>0?Math.min(j,k):k;
        String val = json.substring(i+pat.length(), end).trim();
        if (val.startsWith("\"")) return val.substring(1, val.indexOf('"',1));
        else return val;
    }

    public String getName() { return name; }

}
