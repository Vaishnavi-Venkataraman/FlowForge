package com.flowforge.task.decorator;

import com.flowforge.model.TaskConfig;
import com.flowforge.model.TaskResult;
import com.flowforge.task.Task;

import java.time.Instant;
import java.util.concurrent.*;

/**
 * Decorator that enforces a maximum execution time on any task.
 *
 * WHY: A stuck HTTP call or a long-running DB query can block the entire
 * workflow pipeline. Timeout ensures no single task holds up execution.
 *
 * Composable: new TimeoutDecorator(new RetryDecorator(new HttpTask(...), 2, 500), 5000)
 * → retries up to 2 times, but the ENTIRE retry sequence must finish within 5 seconds.
 */
public class TimeoutDecorator extends TaskDecorator {

    private final long timeoutMs;

    /**
     * @param wrappedTask the task to wrap
     * @param timeoutMs   maximum execution time in milliseconds
     */
    public TimeoutDecorator(Task wrappedTask, long timeoutMs) {
        super(wrappedTask);
        if (timeoutMs <= 0) {
            throw new IllegalArgumentException("Timeout must be positive, got: " + timeoutMs);
        }
        this.timeoutMs = timeoutMs;
    }

    @Override
    public TaskResult execute(TaskConfig config) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<TaskResult> future = executor.submit(() -> wrappedTask.execute(config));

        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            System.out.println("    [TimeoutDecorator] Task " + getName()
                    + " TIMED OUT after " + timeoutMs + "ms");
            return TaskResult.failure("Task timed out after " + timeoutMs + "ms", Instant.now());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return TaskResult.failure("Task interrupted", Instant.now());
        } catch (ExecutionException e) {
            return TaskResult.failure("Task execution error: " + e.getCause().getMessage(), Instant.now());
        } finally {
            executor.shutdown();
        }
    }
}