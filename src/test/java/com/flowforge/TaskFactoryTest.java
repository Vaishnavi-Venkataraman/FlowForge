package com.flowforge;

import com.flowforge.exception.TaskExecutionException;
import com.flowforge.task.Task;
import com.flowforge.task.TaskFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Factory Method pattern — TaskFactory.
 */
class TaskFactoryTest {

    private TaskFactory factory;

    @BeforeEach
    void setUp() {
        factory = new TaskFactory();
    }

    @Test
    void shouldCreateHttpTask() {
        Task task = factory.createTask(new com.flowforge.model.TaskConfig(
                "fetch", "http", java.util.Map.of("url", "https://api.test.com", "method", "GET")));
        assertNotNull(task);
        assertEquals("fetch", task.getName());
    }

    @Test
    void shouldCreateEmailTask() {
        Task task = factory.createTask(new com.flowforge.model.TaskConfig(
                "notify", "email", java.util.Map.of("to", "a@b.com", "subject", "Hi")));
        assertNotNull(task);
        assertEquals("notify", task.getName());
    }

    @Test
    void shouldCreateDatabaseTask() {
        Task task = factory.createTask(new com.flowforge.model.TaskConfig(
                "query", "database", java.util.Map.of("query", "SELECT 1")));
        assertNotNull(task);
    }

    @Test
    void shouldCreateTransformTask() {
        Task task = factory.createTask(new com.flowforge.model.TaskConfig(
                "clean", "transform", java.util.Map.of("input", "data", "operation", "normalize")));
        assertNotNull(task);
    }

    @Test
    void shouldCreateDelayTask() {
        Task task = factory.createTask(new com.flowforge.model.TaskConfig(
                "wait", "delay", java.util.Map.of("seconds", "1")));
        assertNotNull(task);
    }

    @Test
    void shouldThrowOnUnknownType() {
        assertThrows(TaskExecutionException.class, () ->
                factory.createTask(new com.flowforge.model.TaskConfig(
                        "bad", "unknown_type", java.util.Map.of())));
    }

    @Test
    void shouldRegisterCustomTaskType() {
        factory.registerTaskType("custom", name -> new com.flowforge.task.HttpTask(name));
        Task task = factory.createTask(new com.flowforge.model.TaskConfig(
                "my-custom", "custom", java.util.Map.of("url", "http://x.com", "method", "GET")));
        assertNotNull(task);
        assertEquals("my-custom", task.getName());
    }
}