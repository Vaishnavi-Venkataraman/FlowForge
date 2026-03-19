package com.flowforge.task.decorator;

import com.flowforge.model.TaskConfig;
import com.flowforge.model.TaskResult;
import com.flowforge.task.Task;

/**
 * Base decorator for Task — implements the Decorator pattern.
 *
 * WHY (Decorator Pattern):
 * We need to add cross-cutting behavior to tasks:
 *   - Retry logic (retry 3 times with backoff on failure)
 *   - Logging (log entry/exit/duration for each task)
 *   - Metrics (track execution time)
 *   - Timeout (abort if task takes too long)
 *
 * Without Decorator, we would need to:
 *   1. Modify EVERY task class to add retry logic → violates OCP
 *   2. Or put retry/logging in the engine → violates SRP
 *   3. Or use inheritance (RetryHttpTask, RetryEmailTask) → class explosion
 *
 * With Decorator:
 *   - Each behavior is a wrapper class
 *   - Wrappers are composable: RetryDecorator(LoggingDecorator(HttpTask))
 *   - Tasks are unaware they're being decorated
 *   - New behaviors added without changing existing code
 *
 * This base class delegates all methods to the wrapped task.
 * Concrete decorators override execute() to add behavior before/after.
 */
public abstract class TaskDecorator implements Task {

    protected final Task wrappedTask;

    protected TaskDecorator(Task wrappedTask) {
        if (wrappedTask == null) {
            throw new IllegalArgumentException("Wrapped task cannot be null");
        }
        this.wrappedTask = wrappedTask;
    }

    @Override
    public String getName() {
        return wrappedTask.getName();
    }

    @Override
    public String getType() {
        return wrappedTask.getType();
    }

    @Override
    public TaskResult execute(TaskConfig config) {
        return wrappedTask.execute(config);
    }
}