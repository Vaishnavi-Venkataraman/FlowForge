package com.flowforge;

import com.flowforge.model.TaskConfig;
import com.flowforge.model.TaskResult;
import com.flowforge.task.*;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TaskExecutionTest {

    @Test
    void httpTaskShouldSucceed() {
        Task task = new HttpTask("http-test");
        TaskResult result = task.execute(new TaskConfig("http-test", "http",
                Map.of("url", "https://api.test.com", "method", "GET")));
        assertTrue(result.isSuccess());
    }

    @Test
    void httpTaskShouldFailWithoutUrl() {
        Task task = new HttpTask("http-test");
        TaskResult result = task.execute(new TaskConfig("http-test", "http", Map.of()));
        assertFalse(result.isSuccess());
    }

    @Test
    void emailTaskShouldSucceed() {
        Task task = new EmailTask("email-test");
        TaskResult result = task.execute(new TaskConfig("email-test", "email",
                Map.of("to", "user@test.com", "subject", "Hello")));
        assertTrue(result.isSuccess());
    }

    @Test
    void emailTaskShouldFailWithoutTo() {
        Task task = new EmailTask("email-test");
        TaskResult result = task.execute(new TaskConfig("email-test", "email", Map.of()));
        assertFalse(result.isSuccess());
    }

    @Test
    void transformTaskShouldSucceed() {
        Task task = new TransformTask("transform-test");
        TaskResult result = task.execute(new TaskConfig("transform-test", "transform",
                Map.of("input", "data", "operation", "normalize")));
        assertTrue(result.isSuccess());
    }

    @Test
    void databaseTaskShouldSucceed() {
        Task task = new DatabaseTask("db-test");
        TaskResult result = task.execute(new TaskConfig("db-test", "database",
                Map.of("query", "SELECT 1")));
        assertTrue(result.isSuccess());
    }

    @Test
    void delayTaskShouldSucceed() {
        Task task = new DelayTask("delay-test");
        TaskResult result = task.execute(new TaskConfig("delay-test", "delay",
                Map.of("seconds", "0")));
        assertTrue(result.isSuccess());
    }

    @Test
    void taskResultShouldTrackDuration() {
        Task task = new DelayTask("delay-test");
        TaskResult result = task.execute(new TaskConfig("delay-test", "delay",
                Map.of("seconds", "0")));
        assertNotNull(result.getDuration());
    }
}
