package com.flowforge;

import com.flowforge.model.TaskConfig;
import com.flowforge.model.TaskResult;
import com.flowforge.task.DelayTask;
import com.flowforge.task.Task;
import com.flowforge.task.decorator.LoggingDecorator;
import com.flowforge.task.decorator.RetryDecorator;
import com.flowforge.task.decorator.TimeoutDecorator;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DecoratorTest {

    private static final TaskConfig DELAY_CONFIG = new TaskConfig("test-delay", "delay", Map.of("seconds", "0"));

    private Task createDelayTask() {
        return new DelayTask("test-delay");
    }

    @Test
    void loggingDecoratorShouldPreserveName() {
        Task decorated = new LoggingDecorator(createDelayTask());
        assertEquals("test-delay", decorated.getName());
    }

    @Test
    void loggingDecoratorShouldExecute() {
        Task decorated = new LoggingDecorator(createDelayTask());
        TaskResult result = decorated.execute(DELAY_CONFIG);
        assertTrue(result.isSuccess());
    }

    @Test
    void retryDecoratorShouldSucceedOnFirstTry() {
        Task decorated = new RetryDecorator(createDelayTask(), 3, 10);
        TaskResult result = decorated.execute(DELAY_CONFIG);
        assertTrue(result.isSuccess());
    }

    @Test
    void timeoutDecoratorShouldSucceedWithinLimit() {
        Task decorated = new TimeoutDecorator(createDelayTask(), 5000);
        TaskResult result = decorated.execute(DELAY_CONFIG);
        assertTrue(result.isSuccess());
    }

    @Test
    void timeoutDecoratorShouldRejectInvalidTimeout() {
        Task task = createDelayTask();
        assertThrows(IllegalArgumentException.class, () -> new TimeoutDecorator(task, -1));
    }

    @Test
    void shouldStackDecorators() {
        Task task = createDelayTask();
        task = new LoggingDecorator(task);
        task = new RetryDecorator(task, 2, 10);
        task = new TimeoutDecorator(task, 5000);
        TaskResult result = task.execute(DELAY_CONFIG);
        assertTrue(result.isSuccess());
    }
}