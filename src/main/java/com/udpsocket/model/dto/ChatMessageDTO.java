package com.udpsocket.model.dto;

import java.util.*;
import org.json.*;

public class ChatMessageDTO {
    public int id;

    public String getSender() {
        return sender;
    }

    public String getContent() {
        return content;
    }

    public String sender;
    public String receiver;
    public String msgType;     // TEXT, FILE, IMAGE...
    public String content;     // nội dung hoặc fileId
    public String timestamp;

    public ChatMessageDTO() {}

    public ChatMessageDTO(int id, String sender, String receiver,
                          String msgType, String content, String timestamp) {
        this.id = id;
        this.sender = sender;
        this.receiver = receiver;
        this.msgType = msgType;
        this.content = content;
        this.timestamp = timestamp;
    }

    // Parse JSON trả về từ server
    public static List<ChatMessageDTO> parseList(String json) {
        List<ChatMessageDTO> list = new ArrayList<>();

        try {
            JSONObject obj = new JSONObject(json);

            if (!obj.optString("status").equals("OK"))
                return list;

            JSONArray arr = obj.optJSONArray("rows");
            if (arr == null) return list;

            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);

                ChatMessageDTO dto = new ChatMessageDTO(
                        o.optInt("id"),
                        o.optString("sender"),
                        o.optString("receiver"),
                        o.optString("msg_type"),
                        o.optString("content"),
                        o.optString("timestamp")
                );

                list.add(dto);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    @Override
    public String toString() {
        return "[" + timestamp + "] " + sender + ": " + content;
    }
}
