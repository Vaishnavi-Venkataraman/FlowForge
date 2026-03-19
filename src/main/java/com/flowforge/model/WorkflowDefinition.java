package com.flowforge.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Domain model representing a workflow definition.
 *
 * WHY: Replaces the HashMap<String, Object> that represented a workflow
 * in the naive implementation. This is a proper domain object with:
 * - Typed fields instead of string keys
 * - Unique ID generation
 * - Immutable task list (defensive copy)
 * - Status tracking with enum
 */
public class WorkflowDefinition {

    private final String id;
    private final String name;
    private final TriggerConfig trigger;
    private final List<TaskConfig> tasks;
    private final Instant createdAt;
    private WorkflowStatus status;

    public WorkflowDefinition(String name, TriggerConfig trigger, List<TaskConfig> tasks) {
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

    @Override
    public String toString() {
        return "WorkflowDefinition{id='" + id + "', name='" + name
                + "', status=" + status + ", tasks=" + tasks.size() + "}";
    }
}