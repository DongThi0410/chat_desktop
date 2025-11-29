package com.udpsocket.util;

import com.udpsocket.model.dto.ChatMessageDTO;

import java.util.ArrayList;
import java.util.List;

public class JsonHelper {
    /** Parse string field: "key":"value" */
    public static String getString(String json, String key) {
        String pat = "\"" + key + "\":\"";
        int i = json.indexOf(pat);
        if (i < 0) return null;

        int start = i + pat.length();
        int end = json.indexOf("\"", start);
        if (end < 0) return null;

        return json.substring(start, end);
    }

    /** Parse int field: "key":123 */

    public static int getInt(String json, String key) {
        String pat = "\"" + key + "\":";
        int i = json.indexOf(pat);
        if (i < 0) return 0;

        int start = i + pat.length();
        int end1 = json.indexOf(",", start);
        int end2 = json.indexOf("}", start);

        int end = (end1 > 0) ? Math.min(end1, end2) : end2;
        if (end < 0) return 0;

        return Integer.parseInt(json.substring(start, end).trim());
    }
    /** Parse JSON array of strings: ["a","b","c"] */
    public static List<String> parseStringList(String json, String key) {
        List<String> list = new ArrayList<>();

        // Tìm key
        String pattern = "\"" + key + "\":";
        int keyIndex = json.indexOf(pattern);
        if (keyIndex < 0) return list;

        int start = json.indexOf("[", keyIndex);
        int end   = json.indexOf("]", start);
        if (start < 0 || end < 0 || end <= start + 1) return list;

        String inside = json.substring(start + 1, end).trim();
        if (inside.isEmpty()) return list;

        String[] parts = inside.split(",");
        for (String p : parts) {
            p = p.trim();
            if (p.startsWith("\"") && p.endsWith("\"")) {
                list.add(p.substring(1, p.length() - 1));
            }
        }

        return list;
    }

    /** Parse mảng các ChatRow (từ get_chat) */
    public static List<ChatMessageDTO> parseChatRows(String json) {
        List<ChatMessageDTO> list = new ArrayList<>();

        int start = json.indexOf("[");
        int end   = json.lastIndexOf("]");
        if (start < 0 || end < 0) return list;

        String arr = json.substring(start + 1, end).trim();
        if (arr.isEmpty()) return list;

        // Tách object { ... }
        String[] objects = arr.split("\\},\\{");

        for (String raw : objects) {
            String o = raw.replace("{","").replace("}","");

            ChatMessageDTO dto = new ChatMessageDTO();
            dto.id        = getInt(o, "id");
            dto.sender    = getString(o, "sender");
            dto.receiver  = getString(o, "receiver");
            dto.msgType   = getString(o, "msg_type");
            dto.content   = getString(o, "content");
            dto.timestamp = getString(o, "timestamp");

            list.add(dto);
        }

        return list;
    }


    /** Kiểm tra status = "OK" hay không */
    public static boolean isOk(String json) {
        String s = getString(json, "status");
        return "OK".equalsIgnoreCase(s);
    }

    /** Lấy message lỗi */
    public static String getMessage(String json) {
        return getString(json, "message");
    }
}
