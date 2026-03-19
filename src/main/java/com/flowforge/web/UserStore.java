package com.flowforge.web;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory user store with password hashing and session management.
 */
public class UserStore {

    private final Map<String, UserRecord> users = new ConcurrentHashMap<>();
    private final Map<String, String> sessions = new ConcurrentHashMap<>(); // sessionId → username

    public record UserRecord(String username, String passwordHash, String displayName, long createdAt) {}

    /**
     * Registers a new user. Returns error message or null on success.
     */
    public String register(String username, String password, String displayName) {
        if (username == null || username.isBlank()) return "Username is required";
        if (password == null || password.length() < 4) return "Password must be at least 4 characters";
        if (users.containsKey(username.toLowerCase())) return "Username already taken";

        String hash = hashPassword(password);
        users.put(username.toLowerCase(), new UserRecord(
                username.toLowerCase(), hash,
                displayName != null && !displayName.isBlank() ? displayName : username,
                System.currentTimeMillis()
        ));
        return null;
    }

    /**
     * Authenticates and creates a session. Returns session ID or null on failure.
     */
    public String login(String username, String password) {
        if (username == null || password == null) return null;
        UserRecord user = users.get(username.toLowerCase());
        if (user == null) return null;
        if (!user.passwordHash().equals(hashPassword(password))) return null;

        String sessionId = UUID.randomUUID().toString();
        sessions.put(sessionId, username.toLowerCase());
        return sessionId;
    }

    /**
     * Resolves a session to a username. Returns null if invalid.
     */
    public String getUsername(String sessionId) {
        if (sessionId == null) return null;
        return sessions.get(sessionId);
    }

    public UserRecord getUser(String username) {
        return users.get(username.toLowerCase());
    }

    public void logout(String sessionId) {
        sessions.remove(sessionId);
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}