package com.flowforge.exception;

/**
 * Thrown when a task fails during execution.
 */
public class TaskExecutionException extends FlowForgeException {

    private final String taskName;

    public TaskExecutionException(String taskName, String message) {
        super("Task '" + taskName + "' failed: " + message);
        this.taskName = taskName;
    }

    public TaskExecutionException(String taskName, String message, Throwable cause) {
        super("Task '" + taskName + "' failed: " + message, cause);
        this.taskName = taskName;
    }

    public String getTaskName() {
        return taskName;
    }
}