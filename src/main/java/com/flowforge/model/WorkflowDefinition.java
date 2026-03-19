package com.flowforge.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class WorkflowDefinition {

    private final String id;
    private final String name;
    private final TriggerConfig trigger;
    private final List<TaskConfig> tasks;
    private final Instant createdAt;
    private final String executionStrategyName;
    private WorkflowStatus status;

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

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public TriggerConfig getTrigger() {
        return trigger;
    }

    public List<TaskConfig> getTasks() {
        return tasks;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public WorkflowStatus getStatus() {
        return status;
    }

    public void setStatus(WorkflowStatus status) {
        this.status = status;
    }

    public String getExecutionStrategyName() {
        return executionStrategyName;
    }

    @Override
    public String toString() {
        return "WorkflowDefinition{id='" + id + "', name='" + name
                + "', strategy='" + executionStrategyName
                + "', status=" + status + ", tasks=" + tasks.size() + "}";
    }
}