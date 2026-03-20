package com.flowforge.task.decorator;

import com.flowforge.model.TaskConfig;
import com.flowforge.model.TaskResult;
import com.flowforge.task.Task;

import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

/**
 * Decorator that enforces a maximum execution time on any task.
 */
public class TimeoutDecorator extends TaskDecorator {

    private static final Logger LOGGER = Logger.getLogger(TimeoutDecorator.class.getName());
    private final long timeoutMs;

    public TimeoutDecorator(Task wrappedTask, long timeoutMs) {
        super(wrappedTask);
        if (timeoutMs <= 0) {
            throw new IllegalArgumentException("Timeout must be positive, got: " + timeoutMs);
        }
        this.timeoutMs = timeoutMs;
    }

    @Override
    public TaskResult execute(TaskConfig config) {
        // FIX: try-with-resources ensures ExecutorService is always closed
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            Future<TaskResult> future = executor.submit(() -> wrappedTask.execute(config));

            try {
                return future.get(timeoutMs, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                future.cancel(true);
                LOGGER.warning(() -> "Task " + getName() + " TIMED OUT after " + timeoutMs + "ms");
                return TaskResult.failure("Task timed out after " + timeoutMs + "ms", Instant.now());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return TaskResult.failure("Task interrupted", Instant.now());
            } catch (ExecutionException e) {
                return TaskResult.failure("Task execution error: " + e.getCause().getMessage(), Instant.now());
            }
        }
    }
}
