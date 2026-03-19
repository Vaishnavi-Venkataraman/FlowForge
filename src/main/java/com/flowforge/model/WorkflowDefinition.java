package com.flowforge.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/* REFACTORING: Removed public setStatus() - state transitions are now guarded methods*/
public class WorkflowDefinition {

    private final String id;
    private final String name;
    private final TriggerConfig trigger;
    private final List<TaskConfig> tasks;
    private final Instant createdAt;
    private final String executionStrategyName;
    private volatile WorkflowStatus status;  // volatile for thread visibility

    public WorkflowDefinition(String name, TriggerConfig trigger, List<TaskConfig> tasks) {
        this(name, trigger, tasks, "sequential");
    }

    public WorkflowDefinition(String name, TriggerConfig trigger, List<TaskConfig> tasks,
                               String executionStrategyName) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Workflow name cannot be null or blank");
        }
        if (tasks == null || tasks.isEmpty()) {
            throw new IllegalArgumentException("Workflow must have at least one task");
        }
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.trigger = trigger;
        this.tasks = Collections.unmodifiableList(new ArrayList<>(tasks));
        this.createdAt = Instant.now();
        this.status = WorkflowStatus.CREATED;
        this.executionStrategyName = executionStrategyName != null
                ? executionStrategyName : "sequential";
    }

    /**
     * Transitions to RUNNING. Only valid from CREATED or PAUSED.
     *
     * @throws IllegalStateException if current state doesn't allow this transition
     */
    public void markRunning() {
        if (!status.isExecutable()) {
            throw new IllegalStateException(
                    "Cannot start workflow '" + name + "' — current status: " + status);
        }
        this.status = WorkflowStatus.RUNNING;
    }

    /**
     * Transitions to COMPLETED. Only valid from RUNNING.
     */
    public void markCompleted() {
        if (status != WorkflowStatus.RUNNING) {
            throw new IllegalStateException(
                    "Cannot complete workflow '" + name + "' — current status: " + status);
        }
        this.status = WorkflowStatus.COMPLETED;
    }

    /**
     * Transitions to FAILED. Valid from RUNNING or CREATED (pipeline failure).
     */
    public void markFailed() {
        if (status != WorkflowStatus.RUNNING && status != WorkflowStatus.CREATED) {
            throw new IllegalStateException(
                    "Cannot fail workflow '" + name + "' — current status: " + status);
        }
        this.status = WorkflowStatus.FAILED;
    }

    /**
     * Resets to CREATED for re-execution. Only from terminal states.
     */
    public void reset() {
        if (!status.isTerminal()) {
            throw new IllegalStateException(
                    "Cannot reset workflow '" + name + "' — current status: " + status);
        }
        this.status = WorkflowStatus.CREATED;
    }

    // --- Getters (no setStatus!) ---

    public String getId() { return id; }
    public String getName() { return name; }
    public TriggerConfig getTrigger() { return trigger; }
    public List<TaskConfig> getTasks() { return tasks; }
    public Instant getCreatedAt() { return createdAt; }
    public WorkflowStatus getStatus() { return status; }
    public String getExecutionStrategyName() { return executionStrategyName; }

    @Override
    public String toString() {
        return "WorkflowDefinition{id='" + id + "', name='" + name
                + "', strategy='" + executionStrategyName
                + "', status=" + status + ", tasks=" + tasks.size() + "}";
    }
}