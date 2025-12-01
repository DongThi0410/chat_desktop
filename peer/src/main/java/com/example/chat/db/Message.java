package com.example.chat.db;

import java.time.Instant;

public class Message {
    private long id;
    private String fromUser;
    private String toUser;
    private String content;
    private Instant timestamp;
    private boolean isFile;
    private String filePath;

    public Message() {}

    public Message(String fromUser, String toUser, String content, boolean isFile, String filePath) {
        this.fromUser = fromUser;
        this.toUser = toUser;
        this.content = content;
        this.timestamp = Instant.now();
        this.isFile = isFile;
        this.filePath = filePath;
    }

    // getters / setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getFromUser() { return fromUser; }
    public String getToUser() { return toUser; }
    public String getContent() { return content; }
    public Instant getTimestamp() { return timestamp; }
    public boolean isFile() { return isFile; }
    public String getFilePath() { return filePath; }

    public void setFromUser(String fromUser) { this.fromUser = fromUser; }
    public void setToUser(String toUser) { this.toUser = toUser; }
    public void setContent(String content) { this.content = content; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public void setFile(boolean file) { isFile = file; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
}
