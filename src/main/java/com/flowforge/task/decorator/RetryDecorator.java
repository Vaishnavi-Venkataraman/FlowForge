package com.flowforge.task.decorator;

import java.util.logging.Logger;

import com.flowforge.model.TaskConfig;
import com.flowforge.model.TaskResult;
import com.flowforge.task.Task;

public class RetryDecorator extends TaskDecorator {

    private static final Logger LOGGER = Logger.getLogger(RetryDecorator.class.getName());

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

                final int currentAttempt = attempt;
                final long currentDelay = delay;

                LOGGER.info(() -> "    [RetryDecorator] Retry attempt "
                        + currentAttempt + "/" + maxRetries
                        + " for task: " + getName()
                        + " (backoff: " + currentDelay + "ms)");

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

                    final int successAttempt = attempt;

                    LOGGER.info(() -> "    [RetryDecorator] Task "
                            + getName()
                            + " succeeded on attempt "
                            + (successAttempt + 1));
                }

                return result;
            }
        }

        LOGGER.info(() -> "    [RetryDecorator] Task "
                + getName()
                + " FAILED after "
                + (maxRetries + 1)
                + " attempts");

        return result;
    }
}