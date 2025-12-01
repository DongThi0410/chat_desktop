package com.example.chat.listener;


public interface MessageListener {
    void onMessage(String sender, String msg);
    void onFileReceived(String sender, String filename, String absolutePath, long size);
}