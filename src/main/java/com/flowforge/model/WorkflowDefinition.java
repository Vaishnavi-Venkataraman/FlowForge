package com.flowforge.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Domain model representing a workflow definition.
 *
 * REFACTORING (Commit 14):
 * - Removed public setStatus() — state transitions are now guarded methods
 *   that enforce valid transitions (CREATED→RUNNING→COMPLETED/FAILED)
 * - Invalid transitions throw IllegalStateException
 * - All non-status fields are final
 */
public class WorkflowDefinition {

    private final String id;
    private final String name;
    private final TriggerConfig trigger;
    private final List<TaskConfig> tasks;
    private final Instant createdAt;
    private final String executionStrategyName;
    private volatile WorkflowStatus status;

    public WorkflowDefinition(String name, TriggerConfig trigger, List<TaskConfig> tasks) {
        this(name, trigger, tasks, "sequential");
    }

    public WorkflowDefinition(String name, TriggerConfig trigger, List<TaskConfig> tasks,
                               String executionStrategyName) {
        this(null, name, trigger, tasks, executionStrategyName);
    }

    /**
     * Full constructor — allows specifying an existing ID for persistence reload.
     * If existingId is null, a new UUID is generated.
     */
    public WorkflowDefinition(String existingId, String name, TriggerConfig trigger,
                               List<TaskConfig> tasks, String executionStrategyName) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Workflow name cannot be null or blank");
        }
        if (tasks == null || tasks.isEmpty()) {
            throw new IllegalArgumentException("Workflow must have at least one task");
        }
        this.id = (existingId != null) ? existingId : UUID.randomUUID().toString();
        this.name = name;
        this.trigger = trigger;
        this.tasks = Collections.unmodifiableList(new ArrayList<>(tasks));
        this.createdAt = Instant.now();
        this.status = WorkflowStatus.CREATED;
        this.executionStrategyName = executionStrategyName != null
                ? executionStrategyName : "sequential";
    }

    // --- Guarded state transitions ---

    public void markRunning() {
        if (!status.isExecutable()) {
            throw new IllegalStateException(
                    "Cannot start workflow '" + name + "' — current status: " + status);
        }
        this.status = WorkflowStatus.RUNNING;
    }

    public void markCompleted() {
        if (status != WorkflowStatus.RUNNING) {
            throw new IllegalStateException(
                    "Cannot complete workflow '" + name + "' — current status: " + status);
        }
        this.status = WorkflowStatus.COMPLETED;
    }

    public void markFailed() {
        if (status != WorkflowStatus.RUNNING && status != WorkflowStatus.CREATED) {
            throw new IllegalStateException(
                    "Cannot fail workflow '" + name + "' — current status: " + status);
        }
        this.status = WorkflowStatus.FAILED;
    }

    public void reset() {
        if (!status.isTerminal()) {
            throw new IllegalStateException(
                    "Cannot reset workflow '" + name + "' — current status: " + status);
        }
        this.status = WorkflowStatus.CREATED;
    }

    // --- Getters ---

    public String getId() { return id; }
    public String getName() { return name; }
    public TriggerConfig getTrigger() { return trigger; }
    public List<TaskConfig> getTasks() { return tasks; }
    public Instant getCreatedAt() { return createdAt; }
    public WorkflowStatus getStatus() { return status; }
    public String getExecutionStrategyName() { return executionStrategyName; }

    public String getTriggerTypeName() {
        return trigger != null ? trigger.getType().name() : "NONE";
    }

    public String getTriggerValue() {
        return trigger != null ? trigger.getValue() : "";
    }

    @Override
    public String toString() {
        return "WorkflowDefinition{id='" + id + "', name='" + name
                + "', strategy='" + executionStrategyName
                + "', status=" + status + ", tasks=" + tasks.size() + "}";
    }
}