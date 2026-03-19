package com.flowforge.task.decorator;

import com.flowforge.model.TaskConfig;
import com.flowforge.model.TaskResult;
import com.flowforge.task.Task;

/**
 * Decorator that adds retry logic with exponential backoff.
 *
 * WHY: Tasks can fail due to transient issues (network timeout, rate limit,
 * temporary DB lock). Retrying with backoff is a common resilience pattern.
 *
 * Without Decorator: every task would need its own retry loop, or the engine
 * would need retry logic for every task — both violate SRP and DRY.
 *
 * With this Decorator:
 *   Task retryableTask = new RetryDecorator(new HttpTask("Fetch"), 3, 1000);
 *   // HttpTask knows nothing about retries. RetryDecorator handles it.
 *
 * Composable with other decorators:
 *   new RetryDecorator(new LoggingDecorator(new HttpTask("Fetch")), 3, 1000);
 */
public class RetryDecorator extends TaskDecorator {

    private final int maxRetries;
    private final long initialDelayMs;

    /**
     * @param wrappedTask    the task to wrap
     * @param maxRetries     maximum number of retry attempts (0 = no retry)
     * @param initialDelayMs delay before first retry (doubles each attempt)
     */
    public RetryDecorator(Task wrappedTask, int maxRetries, long initialDelayMs) {
        super(wrappedTask);
        if (maxRetries < 0) {
            throw new IllegalArgumentException("maxRetries must be >= 0");
        }
        this.maxRetries = maxRetries;
        this.initialDelayMs = initialDelayMs;
    }

    @Override
    public TaskResult execute(TaskConfig config) {
        TaskResult result = null;
        long delay = initialDelayMs;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            if (attempt > 0) {
                System.out.println("    [RetryDecorator] Retry attempt " + attempt
                        + "/" + maxRetries + " for task: " + getName()
                        + " (backoff: " + delay + "ms)");
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return TaskResult.failure("Retry interrupted", java.time.Instant.now());
                }
                delay *= 2; // Exponential backoff
            }

            result = wrappedTask.execute(config);

            if (result.isSuccess()) {
                if (attempt > 0) {
                    System.out.println("    [RetryDecorator] Task " + getName()
                            + " succeeded on attempt " + (attempt + 1));
                }
                return result;
            }
        }

        System.out.println("    [RetryDecorator] Task " + getName()
                + " FAILED after " + (maxRetries + 1) + " attempts");
        return result;
    }
}