package com.flowforge.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * File-persisted user store with password validation and session management.
 */
public class UserStore {

    private static final Logger LOGGER = Logger.getLogger(UserStore.class.getName());
    private static final Path DATA_DIR = Path.of("data");
    private static final Path USERS_FILE = DATA_DIR.resolve("users.dat");

    private final Map<String, UserRecord> users = new ConcurrentHashMap<>();
    private final Map<String, String> sessions = new ConcurrentHashMap<>();

    public record UserRecord(String username, String passwordHash, String displayName, long createdAt) {}

    public UserStore() {
        loadFromDisk();
    }

    public String register(String username, String password, String displayName) {
        if (username == null || username.isBlank()) {
            return "Username is required";
        }
        if (username.length() < 3) {
            return "Username must be at least 3 characters";
        }
        if (!username.matches("^\\w+$")) {
            return "Username can only contain letters, numbers, and underscores";
        }
        if (users.containsKey(username.toLowerCase())) {
            return "Username '" + username + "' is already taken";
        }

        String pwError = validatePassword(password);
        if (pwError != null) {
            return pwError;
        }

        String hash = hashPassword(password);
        users.put(username.toLowerCase(), new UserRecord(
                username.toLowerCase(), hash,
                displayName != null && !displayName.isBlank() ? displayName : username,
                System.currentTimeMillis()
        ));
        saveToDisk();
        return null;
    }

    /**
     * Validates password strength using simple char-by-char checks
     * instead of regex to avoid ReDoS (polynomial backtracking) vulnerabilities.
     */
    public static String validatePassword(String password) {
        if (password == null || password.length() < 6) {
            return "Password must be at least 6 characters";
        }

        boolean hasDigit = false;
        boolean hasUpper = false;
        boolean hasSpecial = false;

        for (char c : password.toCharArray()) {
            if (Character.isDigit(c)) {
                hasDigit = true;
            } else if (Character.isUpperCase(c)) {
                hasUpper = true;
            } else if (!Character.isLetterOrDigit(c)) {
                hasSpecial = true;
            }
        }

        if (!hasDigit) {
            return "Password must contain at least one number";
        }
        if (!hasSpecial) {
            return "Password must contain at least one special character (!@#$%^&* etc.)";
        }
        if (!hasUpper) {
            return "Password must contain at least one uppercase letter";
        }
        return null;
    }

    public String login(String username, String password) {
        if (username == null || password == null) {
            return null;
        }
        UserRecord user = users.get(username.toLowerCase());
        if (user == null) {
            return null;
        }
        if (!user.passwordHash().equals(hashPassword(password))) {
            return null;
        }

        String sessionId = UUID.randomUUID().toString();
        sessions.put(sessionId, username.toLowerCase());
        return sessionId;
    }

    public String getUsername(String sessionId) {
        if (sessionId == null) {
            return null;
        }
        return sessions.get(sessionId);
    }

    public UserRecord getUser(String username) {
        return users.get(username.toLowerCase());
    }

    public void logout(String sessionId) {
        sessions.remove(sessionId);
    }

    private void saveToDisk() {
        try {
            Files.createDirectories(DATA_DIR);
            try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(USERS_FILE))) {
                for (UserRecord user : users.values()) {
                    pw.println(user.username() + "\t" + user.passwordHash() + "\t"
                            + user.displayName() + "\t" + user.createdAt());
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to save users", e);
        }
    }

    private void loadFromDisk() {
        if (!Files.exists(USERS_FILE)) {
            return;
        }
        try {
            List<String> lines = Files.readAllLines(USERS_FILE);
            for (String line : lines) {
                String[] parts = line.split("\t");
                if (parts.length >= 4) {
                    users.put(parts[0], new UserRecord(parts[0], parts[1], parts[2], Long.parseLong(parts[3])));
                }
            }
            LOGGER.info(() -> "Loaded " + users.size() + " users from disk");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load users", e);
        }
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
