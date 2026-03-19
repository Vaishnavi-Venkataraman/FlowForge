package com.flowforge.model;

/**
 * Represents the lifecycle states of a single task execution.
 */
public enum TaskStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    SKIPPED
}