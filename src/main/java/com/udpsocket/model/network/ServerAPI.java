package com.udpsocket.model.network;

import com.udpsocket.model.dto.ChatMessageDTO;
import com.udpsocket.util.JsonHelper;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.URLEncoder;
import java.util.List;

public class ServerAPI {

    private final String host;
    private final int port;

    public ServerAPI(String host, int port) {
        this.host = host;
        this.port = port;
    }

    private String sendRequest(String json) throws IOException {
        Socket socket = new Socket(host, port);

        DataInputStream dis = new DataInputStream(socket.getInputStream());
        DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

        dos.writeUTF(json);
        dos.flush();

        String response = dis.readUTF();

        socket.close();

        return response;
    }

    public List<ChatMessageDTO> getChatHistory(String me, String peer) {
        try {
            String req = "{ \"action\":\"get_chat\", \"me\":\""+me+"\", \"peer\":\""+peer+"\" }";
            String resp = sendRequest(req);

            return ChatMessageDTO.parseList(resp); // parse JSON from server

        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }
    public void saveMessage(String sender, String receiver, String msg) {
        try {
            String req = "{ \"action\":\"save_msg\", \"from\":\""+sender+"\", \"to\":\""+receiver+"\", \"msg\":\""+msg+"\" }";
            sendRequest(req);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public List<String> getPeers(String me) {
        try {
            String req = "{ \"action\":\"get_peers\", \"me\":\"" + me + "\" }";
            String resp = sendRequest(req);
            return JsonHelper.parseStringList(resp, "peers"); // chỉ định key
        } catch (Exception e) {
            return List.of();
        }
    }

    public String uploadFile(File file, String from, String to) throws IOException{
        return "http://server.com/files/"+ URLEncoder.encode(file.getName(), "UTF-8");
    }


    public void logout(String name) {
        try {
            String req = "{ \"action\":\"logout\", \"name\":\""+name+"\" }";
            sendRequest(req);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
