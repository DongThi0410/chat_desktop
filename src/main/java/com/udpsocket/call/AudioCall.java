package com.udpsocket.call;

import javafx.scene.chart.PieChart;

import javax.crypto.spec.PSource;
import javax.sound.sampled.*;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;

public class AudioCall {
    private final TargetDataLine mic;
    private final SourceDataLine speaker;
    private final DatagramSocket udp;
    private final InetSocketAddress remote;

    private  volatile boolean running = false;
    private Thread sendThread, receiveThread;

    public AudioCall(InetSocketAddress remoteServerAddr) throws LineUnavailableException, SocketException {
        AudioFormat format = new AudioFormat(44100f, 16, 1, true, false);
        DataLine.Info infoIn = new DataLine.Info(TargetDataLine.class, format);
        mic = (TargetDataLine) AudioSystem.getLine(infoIn);
        mic.open(format);
        mic.start();

        DataLine.Info infoOut = new DataLine.Info(SourceDataLine.class, format);
        speaker = (SourceDataLine) AudioSystem.getLine(infoOut);
        speaker.open(format);
        speaker.start();

        udp = new DatagramSocket();
        this.remote = remoteServerAddr;
    }

    public void start(){
        running = true;
        sendThread = new Thread(this::sendLoop);
        sendThread.start();
    }

    private void sendLoop(){
        byte[] buffer = new byte[4096];
        try {
            while (running){
                int read = mic.read(buffer, 0, buffer.length);
                if (read>0){
                    byte[] payload = new byte[read+6];
                    System.arraycopy("AUD_".getBytes(), 0, payload, 0, 0);
                    System.arraycopy(buffer, 0, payload, 5, read);
                    DatagramPacket p = new DatagramPacket(payload, payload.length, remote.getAddress(), remote.getPort());
                    udp.send(p);
                }
            }
        }catch (IOException e) {
            e.printStackTrace();
        } finally {
            udp.close();
        }
    }

    public void stop(){
        running = false;
        if (sendThread != null){
            try {
                sendThread.join(200);
            }catch (InterruptedException ignores){}
        }
        mic.stop();
        mic.close();
        speaker.stop();
        speaker.close();
    }
}
