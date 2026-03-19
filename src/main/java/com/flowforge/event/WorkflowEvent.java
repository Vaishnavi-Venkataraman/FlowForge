package com.flowforge.event;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a domain event in the FlowForge system.
 * This implements the Observer/Pub-Sub architectural pattern.
 */
public class WorkflowEvent {

    /**
     * Event types that the system emits.
     */
    public enum Type {
        WORKFLOW_REGISTERED,
        WORKFLOW_STARTED,
        WORKFLOW_COMPLETED,
        WORKFLOW_FAILED,
        TASK_STARTED,
        TASK_COMPLETED,
        TASK_FAILED,
        TASK_SKIPPED
    }

    private final Type type;
    private final String workflowName;
    private final String workflowId;
    private final String taskName;
    private final String message;
    private final Instant timestamp;
    private final Map<String, String> metadata;

    private WorkflowEvent(Type type, String workflowName, String workflowId,
                           String taskName, String message, Map<String, String> metadata) {
        this.type = type;
        this.workflowName = workflowName;
        this.workflowId = workflowId;
        this.taskName = taskName;
        this.message = message;
        this.timestamp = Instant.now();
        this.metadata = metadata != null
                ? Collections.unmodifiableMap(new HashMap<>(metadata))
                : Collections.emptyMap();
    }

    // --- Factory methods for workflow-level events ---

    public static WorkflowEvent workflowRegistered(String name, String id) {
        return new WorkflowEvent(Type.WORKFLOW_REGISTERED, name, id, null,
                "Workflow registered: " + name, null);
    }

    public static WorkflowEvent workflowStarted(String name, String id, String strategyName) {
        return new WorkflowEvent(Type.WORKFLOW_STARTED, name, id, null,
                "Workflow started: " + name + " [strategy=" + strategyName + "]",
                Map.of("strategy", strategyName));
    }

    public static WorkflowEvent workflowCompleted(String name, String id, int taskCount) {
        return new WorkflowEvent(Type.WORKFLOW_COMPLETED, name, id, null,
                "Workflow completed: " + name + " (" + taskCount + " tasks)",
                Map.of("taskCount", String.valueOf(taskCount)));
    }

    public static WorkflowEvent workflowFailed(String name, String id, String error) {
        return new WorkflowEvent(Type.WORKFLOW_FAILED, name, id, null,
                "Workflow FAILED: " + name + " — " + error,
                Map.of("error", error));
    }

    // --- Factory methods for task-level events ---

    public static WorkflowEvent taskStarted(String workflowName, String workflowId, String taskName) {
        return new WorkflowEvent(Type.TASK_STARTED, workflowName, workflowId, taskName,
                "Task started: " + taskName, null);
    }

    public static WorkflowEvent taskCompleted(String workflowName, String workflowId,
                                               String taskName, long durationMs) {
        return new WorkflowEvent(Type.TASK_COMPLETED, workflowName, workflowId, taskName,
                "Task completed: " + taskName + " (" + durationMs + "ms)",
                Map.of("durationMs", String.valueOf(durationMs)));
    }

    public static WorkflowEvent taskFailed(String workflowName, String workflowId,
                                            String taskName, String error) {
        return new WorkflowEvent(Type.TASK_FAILED, workflowName, workflowId, taskName,
                "Task FAILED: " + taskName + " — " + error,
                Map.of("error", error));
    }

    public static WorkflowEvent taskSkipped(String workflowName, String workflowId, String taskName) {
        return new WorkflowEvent(Type.TASK_SKIPPED, workflowName, workflowId, taskName,
                "Task skipped: " + taskName, null);
    }

    // --- Getters ---

    public Type getType() { return type; }
    public String getWorkflowName() { return workflowName; }
    public String getWorkflowId() { return workflowId; }
    public String getTaskName() { return taskName; }
    public String getMessage() { return message; }
    public Instant getTimestamp() { return timestamp; }
    public Map<String, String> getMetadata() { return metadata; }

    @Override
    public String toString() {
        return "[" + type + "] " + message;
    }
}