package com.udpsocket.model.entity;

import java.sql.Timestamp;

public class ChatRow {
    public final String sender, receiver, content, msgType, fileUUID, fileName;
    public final long fileSize;

    public ChatRow(String sender, String receiver, String content, String msgType, String fileUUID, String fileName, long fileSize, Timestamp ts) {
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
        this.msgType = msgType;
        this.fileUUID = fileUUID;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.ts = ts;
    }

    public final Timestamp ts;


}
