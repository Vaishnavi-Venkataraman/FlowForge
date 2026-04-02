package com.flowforge;

import com.flowforge.exception.TaskExecutionException;
import com.flowforge.model.TaskConfig;
import com.flowforge.task.HttpTask;
import com.flowforge.task.Task;
import com.flowforge.task.TaskFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TaskFactoryTest {

    private TaskFactory factory;

    @BeforeEach
    void setUp() {
        factory = new TaskFactory();
    }

    @Test
    void shouldCreateHttpTask() {
        Task task = factory.createTask(new TaskConfig("fetch", "http", Map.of("url", "https://api.test.com", "method", "GET")));
        assertNotNull(task);
        assertEquals("fetch", task.getName());
    }

    @Test
    void shouldCreateEmailTask() {
        Task task = factory.createTask(new TaskConfig("notify", "email", Map.of("to", "a@b.com", "subject", "Hi")));
        assertNotNull(task);
        assertEquals("notify", task.getName());
    }

    @Test
    void shouldCreateDatabaseTask() {
        Task task = factory.createTask(new TaskConfig("query", "database", Map.of("query", "SELECT 1")));
        assertNotNull(task);
    }

    @Test
    void shouldCreateTransformTask() {
        Task task = factory.createTask(new TaskConfig("clean", "transform", Map.of("input", "data", "operation", "normalize")));
        assertNotNull(task);
    }

    @Test
    void shouldCreateDelayTask() {
        Task task = factory.createTask(new TaskConfig("wait", "delay", Map.of("seconds", "1")));
        assertNotNull(task);
    }

    @Test
    void shouldThrowOnUnknownType() {
        TaskConfig badConfig = new TaskConfig("bad", "unknown_type", Map.of());
        assertThrows(TaskExecutionException.class, () -> factory.createTask(badConfig));
    }

    @Test
    void shouldRegisterCustomTaskType() {
        factory.registerTaskType("custom", HttpTask::new);
        Task task = factory.createTask(new TaskConfig("my-custom", "custom", Map.of("url", "http://x.com", "method", "GET")));
        assertNotNull(task);
        assertEquals("my-custom", task.getName());
    }
}