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
        TaskConfig config = new TaskConfig("test-delay", "delay", Map.of("seconds", "0"));
        TaskResult result = decorated.execute(config);
        assertTrue(result.isSuccess());
    }

    @Test
    void retryDecoratorShouldSucceedOnFirstTry() {
        Task decorated = new RetryDecorator(createDelayTask(), 3, 10);
        TaskConfig config = new TaskConfig("test-delay", "delay", Map.of("seconds", "0"));
        TaskResult result = decorated.execute(config);
        assertTrue(result.isSuccess());
    }

    @Test
    void timeoutDecoratorShouldSucceedWithinLimit() {
        Task decorated = new TimeoutDecorator(createDelayTask(), 5000);
        TaskConfig config = new TaskConfig("test-delay", "delay", Map.of("seconds", "0"));
        TaskResult result = decorated.execute(config);
        assertTrue(result.isSuccess());
    }

    @Test
    void timeoutDecoratorShouldRejectInvalidTimeout() {
        assertThrows(IllegalArgumentException.class,
                () -> new TimeoutDecorator(createDelayTask(), -1));
    }

    @Test
    void shouldStackDecorators() {
        Task task = createDelayTask();
        task = new LoggingDecorator(task);
        task = new RetryDecorator(task, 2, 10);
        task = new TimeoutDecorator(task, 5000);
        TaskConfig config = new TaskConfig("test-delay", "delay", Map.of("seconds", "0"));
        TaskResult result = task.execute(config);
        assertTrue(result.isSuccess());
    }
}
