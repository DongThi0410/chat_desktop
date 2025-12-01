package com.example.chat.core;


import java.io.*;
import java.net.Socket;
import java.net.ServerSocket;

public class MessageHandler {
    private final String selfName;
    private final ServerSocket serverSocket;
    private volatile boolean running = true;

    // callbacks
    public interface MsgCallback { void onMessage(String sender, String message); }
    public interface FileCallback { void onFileReceived(String sender, String filename, String absPath, long size); }

    private final MsgCallback msgCallback;
    private final FileCallback fileCallback;

    public MessageHandler(String selfName, ServerSocket serverSocket,
                          MsgCallback msgCallback, FileCallback fileCallback) {
        this.selfName = selfName;
        this.serverSocket = serverSocket;
        this.msgCallback = msgCallback;
        this.fileCallback = fileCallback;
        startAcceptLoop();
    }

    private void startAcceptLoop() {
        new Thread(() -> {
            while (running) {
                try {
                    Socket s = serverSocket.accept();
                    handleIncoming(s);
                } catch (IOException e) {
                    if (running) System.err.println("[MessageHandler accept] " + e.getMessage());
                }
            }
        }, "msg-accept-thread").start();
    }

    private void handleIncoming(Socket s) {
        try (DataInputStream dis = new DataInputStream(s.getInputStream())) {
            String type = dis.readUTF();
            if ("MSG".equals(type)) {
                String sender = dis.readUTF();
                String content = dis.readUTF();
                if (msgCallback != null) msgCallback.onMessage(sender, content);
            } else if ("FILE".equals(type)) {
                String sender = dis.readUTF();
                String filename = dis.readUTF();
                long fileSize = dis.readLong();

                File outFile = new File("receive_" + System.currentTimeMillis() + "_" + filename);
                try (FileOutputStream fos = new FileOutputStream(outFile)) {
                    byte[] buf = new byte[4096];
                    long received = 0;
                    int read;
                    while (received < fileSize && (read = dis.read(buf)) != -1) {
                        fos.write(buf, 0, read);
                        received += read;
                    }
                }
                if (fileCallback != null) fileCallback.onFileReceived(sender, filename, outFile.getAbsolutePath(), fileSize);
            } else {
                System.out.println("[MessageHandler] Unknown type: " + type);
            }
        } catch (IOException e) {
            System.err.println("[MessageHandler error] " + e.getMessage());
        } finally {
            try { s.close(); } catch (IOException ignored) {}
        }
    }

    // send text to address "ip:port"
    public void sendText(String address, String message) {
        String[] p = address.split(":", 2);
        if (p.length != 2) {
            System.err.println("[sendText] Bad address: " + address);
            return;
        }
        try (Socket socket = new Socket(p[0], Integer.parseInt(p[1]));
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream())) {
            dos.writeUTF("MSG");
            dos.writeUTF(selfName);
            dos.writeUTF(message);
            dos.flush();
        } catch (IOException e) {
            System.err.println("[Error sending msg] " + e.getMessage());
        }
    }

    // send file to address "ip:port"
    public void sendFile(String address, String filePath) throws IOException {
        String[] p = address.split(":", 2);
        if (p.length != 2) throw new IOException("Bad address: " + address);
        File file = new File(filePath);
        if (!file.exists()) throw new IOException("File not found: " + filePath);

        try (Socket socket = new Socket(p[0], Integer.parseInt(p[1]));
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             FileInputStream fis = new FileInputStream(file)) {

            dos.writeUTF("FILE");
            dos.writeUTF(selfName);
            dos.writeUTF(file.getName());
            dos.writeLong(file.length());

            byte[] buffer = new byte[4096];
            int bytes;
            while ((bytes = fis.read(buffer)) != -1) {
                dos.write(buffer, 0, bytes);
            }
            dos.flush();
            System.out.println("[File sent] " + file.getName() + " (" + file.length() + " bytes)");
        } catch (IOException e) {
            throw e;
        }
    }

    public void stop() {
        running = false;
        try { serverSocket.close(); } catch (IOException ignored) {}
    }
}
