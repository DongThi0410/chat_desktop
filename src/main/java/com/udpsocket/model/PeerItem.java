package com.udpsocket.model;
public class PeerItem {
    private final String username;
    private boolean online;

    public PeerItem(String username, boolean online) {
        this.username = username;
        this.online = online;
    }

    public String getUsername() { return username; }
    public boolean isOnline() { return online; }
    public void setOnline(boolean b) { this.online = b; }

    @Override
    public String toString() {
        return username;
    }
}

