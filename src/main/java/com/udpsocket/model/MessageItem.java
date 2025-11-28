package com.udpsocket.model;

public class MessageItem {

    private final boolean isFile;
    private final String sender;
    private final String text;
    private final String fileUuid;
    private final String fileName;

    public MessageItem(String sender, String text) {
        this.sender = sender;
        this.text = text;
        this.fileUuid = null;
        this.fileName = null;
        this.isFile = false;
    }

    public MessageItem(String sender, String fileUuid, String fileName) {
        this.sender = sender;
        this.fileUuid = fileUuid;
        this.fileName = fileName;
        this.text = null;
        this.isFile = true;
    }

    public boolean isFile() { return isFile; }
    public String getSender() { return sender; }
    public String getText() { return text; }
    public String getFileUuid() { return fileUuid; }
    public String getFileName() { return fileName; }
}
