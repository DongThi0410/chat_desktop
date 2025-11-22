package com.udpsocket.call;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;

import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.FrameGrabber.Exception;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;

import javax.imageio.ImageIO;

public class VideoCall {
    private final InetSocketAddress remote;
    private final DatagramSocket udp;
    private volatile boolean running = false;
    private FrameGrabber grabber;
    private Thread sendThread;

    public VideoCall(InetSocketAddress remote) throws SocketException{
        this.remote = remote;
        this.udp = new DatagramSocket();
    }

    public void start() throws Exception{
        grabber = FrameGrabber.createDefault(0);
        grabber.start();
        running = true;
        sendThread = new Thread(this::sendLoop);
    }

    private void sendLoop(){
        try {
            OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();
            Java2DFrameConverter java2DFrameConverter = new Java2DFrameConverter();

            while (running){
                Frame f = grabber.grab();
                if (f==null) continue;
                BufferedImage img = java2DFrameConverter.convert(f);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(img, "jpg", baos);
                byte[] jpg = baos.toByteArray();

                byte[] payload = new byte[5+jpg.length];
                System.arraycopy("VID__".getBytes(), 0, payload, 0, 5 );
                System.arraycopy(jpg, 0, payload, 5, jpg.length);

                DatagramPacket p = new DatagramPacket(payload, payload.length, remote.getPort());
                udp.send(p);

                Thread.sleep(80);
            }
            grabber.stop();
        }catch (IOException | InterruptedException exception){
            exception.printStackTrace();
        }
    }

    public void stop(){
        running = false;
        if (sendThread != null){
            try {
                sendThread.join(200);
            }catch (InterruptedException ignored){}
        }
    }
}
