package com.udpsocket.helpers;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class MySqlHelper {
    private static final String URL = "jdbc:mysql://localhost:3307/chat?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASS = "";

    private MySqlHelper() {
    }

    public static void upsertPeerOnline(String username, String ip, int port) {
        String sql = "INSERT INTO peers_online(username, ip, port, last_seen) " +
                "VALUES (?, ?, ?, CURRENT_TIMESTAMP) " +
                "ON DUPLICATE KEY UPDATE ip = VALUES(ip), port = VALUES(port), last_seen = CURRENT_TIMESTAMP";

        try (Connection c = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, ip);
            ps.setInt(3, port);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public static void removePeer(String username) {
        String sql = "DELETE peers_online users WHERE username = ? AND port = ?";
        try (Connection c = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static List<PeerInfo> getPeersExcept(String username) {
        String sql = "SELECT username, ip, port FROM peers_online WHERE username <> ?";
        List<PeerInfo> res = new ArrayList<>();
        try (Connection c = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                res.add(new PeerInfo(rs.getString(1), rs.getString(2), rs.getInt(3)));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return res;
    }

    public static void saveMessage(String sender, String receiver, String content) {
        String sql = "INSERT INTO messages(sender, receiver, content) VALUES (?, ?, ?)";
        try (Connection c = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, sender);
            ps.setString(2, receiver);
            ps.setString(3, content);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static List<ChatRow> loadChat(String a, String b, int limit) {
        String sql = "SELECT sender, receiver, content, timestamp  FROM messages " +   // thêm space ở đây
                "WHERE (sender = ? AND receiver = ?) OR (sender = ? AND receiver = ?) " +
                "ORDER BY timestamp  ASC LIMIT ?";

        List<ChatRow> list = new ArrayList<>();

        try (Connection c = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, a);
            ps.setString(2, b);
            ps.setString(3, b);
            ps.setString(4, a);
            ps.setInt(5, limit);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new ChatRow(
                        rs.getString(1),
                        rs.getString(2),
                        rs.getString(3),
                        rs.getTimestamp(4)
                ));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    public static List<String> loadChatPartners(String me) {
        String sql = """
            SELECT DISTINCT CASE WHEN sender = ? THEN receiver ELSE sender END AS peer
            FROM messages
            WHERE sender = ? OR receiver = ?
            """;
        List<String> list = new ArrayList<>();
        try (Connection c = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, me);
            ps.setString(2, me);
            ps.setString(3, me);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(rs.getString("peer"));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public static Optional<PeerOnline> loadPeerOnline(String username) {
        String sql = "SELECT username, ip, port, last_seen FROM peers_online WHERE username = ?";
        try (Connection c = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                PeerOnline po = new PeerOnline(
                        rs.getString("username"),
                        rs.getString("ip"),
                        rs.getInt("port"),
                        rs.getTimestamp("last_seen")
                );
                return Optional.of(po);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return Optional.empty();
    }

    public static void removePeerByName(String username) {
        String sql = "DELETE FROM peers_online WHERE username = ?";
        try (Connection c = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static class PeerInfo {
        public final String username;
        public final String ip;
        public final int port;
        public PeerInfo(String u, String i, int p) { username = u; ip = i; port = p; }
    }

    public static class ChatRow {
        public final String sender, receiver, content;
        public final Timestamp ts;
        public ChatRow(String s, String r, String c, Timestamp t) { sender = s; receiver = r; content = c; ts = t; }
    }

    public static class PeerOnline {
        public final String username;
        public final String ip;
        public final int port;
        public final Timestamp lastSeen;
        public PeerOnline(String u, String ip, int p, Timestamp t) {
            this.username = u; this.ip = ip; this.port = p; this.lastSeen = t;
        }
    }
}
