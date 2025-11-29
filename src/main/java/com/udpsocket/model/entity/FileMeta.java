package com.udpsocket.model.entity;

import org.json.JSONObject;

public class FileMeta {
    public String from;
    public String to;
    public String filename;
    public long size;
    public String url;

    public FileMeta(String from, String to, String filename, long size, String url) {
        this.from = from;
        this.to = to;
        this.filename = filename;
        this.size = size;
        this.url = url;
    }


    public static FileMeta fromJson(String json) {
        JSONObject obj = new JSONObject(json);
        String from = obj.getString("from");
        String to = obj.getString("to");
        String filename = obj.getString("filename");
        long size = obj.getLong("size");
        String url = obj.getString("url");
        return new FileMeta(from, to, filename, size, url);
    }
}
