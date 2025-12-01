package com.example.chat.db;

import java.nio.file.Paths;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ChatDatabase {
    private final String url;

    public ChatDatabase(String dbPath) {
        // dbPath e.g. "./chat_history.db" or System.getProperty("user.home") + "/.p2p_chat.db"
        this.url = "jdbc:sqlite:" + dbPath;
        init();
    }

    private void init() {
        // create table if not exists
        String sql = "CREATE TABLE IF NOT EXISTS messages (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "from_user TEXT NOT NULL," +
                "to_user TEXT NOT NULL," +
                "content TEXT," +
                "timestamp INTEGER NOT NULL," + // epoch seconds
                "is_file INTEGER DEFAULT 0," +
                "file_path TEXT" +
                ");";
        try (Connection c = DriverManager.getConnection(url);
             Statement s = c.createStatement()) {
            s.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Cannot initialize DB: " + e.getMessage(), e);
        }
    }

    public void insertMessage(Message m) {
        String sql = "INSERT INTO messages(from_user, to_user, content, timestamp, is_file, file_path) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection c = DriverManager.getConnection(url);
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, m.getFromUser());
            ps.setString(2, m.getToUser());
            ps.setString(3, m.getContent());
            ps.setLong(4, m.getTimestamp().getEpochSecond());
            ps.setInt(5, m.isFile() ? 1 : 0);
            ps.setString(6, m.getFilePath());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) m.setId(rs.getLong(1));
            }
        } catch (SQLException e) {
            System.err.println("[ChatDatabase] insert error: " + e.getMessage());
        }
    }

    /**
     * Load conversation between localUser and peerName, ordered ascending by time.
     * limit = number of messages to load (most recent)
     */
    public List<Message> loadConversationAsc(String localUser, String peerName, int limit) {
        List<Message> out = new ArrayList<>();
        String sql = """
            SELECT * FROM messages
            WHERE (from_user = ? AND to_user = ?) OR (from_user = ? AND to_user = ?)
            ORDER BY timestamp ASC
            LIMIT ?
        """;

        try (Connection c = DriverManager.getConnection(url);
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, localUser);
            ps.setString(2, peerName);
            ps.setString(3, peerName);
            ps.setString(4, localUser);
            ps.setInt(5, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Message m = new Message(
                            rs.getString("from_user"),
                            rs.getString("to_user"),
                            rs.getString("content"),
                            rs.getInt("is_file") == 1,
                            rs.getString("file_path")
                    );
                    m.setId(rs.getLong("id"));
                    m.setTimestamp(Instant.ofEpochSecond(rs.getLong("timestamp")));
                    out.add(m);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return out;
    }
}
