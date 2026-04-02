package com.flowforge;

import com.flowforge.web.UserStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for UserStore — registration, validation, login, BCrypt hashing.
 * Each test gets a clean state by deleting users.dat before running.
 */
class UserStoreTest {

    private UserStore store;

    @BeforeEach
    void setUp() throws IOException {
        // Clean up persisted users so tests are independent
        Files.deleteIfExists(Path.of("data", "users.dat"));
        store = new UserStore();
    }

    @Test
    void shouldRegisterValidUser() {
        String error = store.register("testuser", "Pass@123", "Test User");
        assertNull(error);
    }

    @Test
    void shouldRejectShortUsername() {
        String error = store.register("ab", "Pass@123", "X");
        assertNotNull(error);
        assertTrue(error.contains("3 characters"));
    }

    @Test
    void shouldRejectDuplicateUsername() {
        store.register("alice", "Pass@123", "Alice");
        String error = store.register("alice", "Pass@456", "Alice2");
        assertNotNull(error);
        assertTrue(error.contains("already taken"));
    }

    @Test
    void shouldRejectWeakPassword() {
        assertNotNull(store.register("user1", "short", "U"));
        assertNotNull(store.register("user2", "nouppercase1!", "U"));
        assertNotNull(store.register("user3", "NoSpecial1", "U"));
        assertNotNull(store.register("user4", "NoDigit!AA", "U"));
    }

    @Test
    void shouldLoginWithCorrectPassword() {
        store.register("bob", "Secure@99", "Bob");
        String sessionId = store.login("bob", "Secure@99");
        assertNotNull(sessionId);
    }

    @Test
    void shouldRejectWrongPassword() {
        store.register("bob2", "Secure@99", "Bob");
        String sessionId = store.login("bob2", "WrongPass@1");
        assertNull(sessionId);
    }

    @Test
    void shouldResolveSession() {
        store.register("carol", "Test@123", "Carol");
        String sessionId = store.login("carol", "Test@123");
        assertEquals("carol", store.getUsername(sessionId));
    }

    @Test
    void shouldLogout() {
        store.register("dave", "Test@123", "Dave");
        String sessionId = store.login("dave", "Test@123");
        store.logout(sessionId);
        assertNull(store.getUsername(sessionId));
    }

    @Test
    void shouldReturnNullForInvalidSession() {
        assertNull(store.getUsername("nonexistent-session"));
        assertNull(store.getUsername(null));
    }
}