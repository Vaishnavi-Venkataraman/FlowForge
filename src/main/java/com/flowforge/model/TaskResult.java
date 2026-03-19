package com.flowforge.model;

import java.time.Duration;
import java.time.Instant;

/**
 * Immutable result of a task execution.
 *
 * WHY: Replaces the pattern of shoving "result" and "status" keys into
 * a HashMap. This gives type safety and clear semantics for what a task
 * execution produces.
 */
public class TaskResult {

    private final TaskStatus status;
    private final String output;
    private final String errorMessage;
    private final Instant startTime;
    private final Instant endTime;

    private TaskResult(TaskStatus status, String output, String errorMessage,
                       Instant startTime, Instant endTime) {
        this.status = status;
        this.output = output;
        this.errorMessage = errorMessage;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public static TaskResult success(String output, Instant startTime) {
        return new TaskResult(TaskStatus.COMPLETED, output, null, startTime, Instant.now());
    }

    public static TaskResult failure(String errorMessage, Instant startTime) {
        return new TaskResult(TaskStatus.FAILED, null, errorMessage, startTime, Instant.now());
    }

    public TaskStatus getStatus() {
        return status;
    }

    public String getOutput() {
        return output;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Duration getDuration() {
        if (startTime != null && endTime != null) {
            return Duration.between(startTime, endTime);
        }
        return Duration.ZERO;
    }

    public boolean isSuccess() {
        return status == TaskStatus.COMPLETED;
    }

    @Override
    public String toString() {
        if (isSuccess()) {
            return "TaskResult{COMPLETED, output='" + output + "', duration=" + getDuration().toMillis() + "ms}";
        }
        return "TaskResult{FAILED, error='" + errorMessage + "'}";
    }
}