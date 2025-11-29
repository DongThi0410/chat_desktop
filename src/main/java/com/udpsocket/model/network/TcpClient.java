package com.udpsocket.model.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class TcpClient {

    private Socket socket; //s tcp conn -> serv
    private PrintWriter out; // text -> serv
    private BufferedReader in; //text <- serv


    //khoi tao conn tcp
    public TcpClient(String host, int port) throws IOException {
        socket = new Socket(host, port);//open socket
        out = new PrintWriter(socket.getOutputStream(), true);//create writer send text(auto flush each time printn
        in = new BufferedReader(new InputStreamReader(socket.getInputStream())); //tao reader de doc text<-serv
    }
//gui String  data -> serv
    public void send(String data){
        out.println(data);//read 1 line (serve must send by println)
    }
    //wait and recv 1 line text <-serv
    public String receive()throws IOException{
        return in.readLine();
    }

    public void close()throws IOException{
        socket.close();
    }


}
