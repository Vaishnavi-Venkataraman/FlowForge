package com.flowforge.model;

/**
 * Represents the lifecycle states of a workflow execution.
 *
 * WHY: Replaces magic strings like "completed", "failed", "running"
 * from the naive implementation. Enums provide compile-time type safety
 * and make invalid states unrepresentable.
 */
public enum WorkflowStatus {
    CREATED,
    RUNNING,
    COMPLETED,
    FAILED,
    PAUSED;

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED;
    }

    public boolean isExecutable() {
        return this == CREATED || this == PAUSED;
    }
}