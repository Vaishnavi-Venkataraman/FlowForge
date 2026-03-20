package com.flowforge.engine;

import com.flowforge.model.TaskConfig;
import com.flowforge.model.TriggerConfig;
import com.flowforge.model.TriggerType;
import com.flowforge.model.WorkflowDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WorkflowBuilder {

    private String name;
    private TriggerConfig trigger;
    private final List<TaskConfig> tasks = new ArrayList<>();
    private String strategyName = "sequential";

    private WorkflowBuilder() {
        // Use static factory method
    }

    /**
     * Entry point for building a workflow.
     */
    public static WorkflowBuilder create() {
        return new WorkflowBuilder();
    }

    /**
     * Sets the workflow name (required).
     */
    public WorkflowBuilder name(String name) {
        this.name = name;
        return this;
    }

    // --- Trigger methods ---

    public WorkflowBuilder trigger(TriggerType type, String value) {
        this.trigger = new TriggerConfig(type, value);
        return this;
    }

    public WorkflowBuilder cronTrigger(String cronExpression) {
        return trigger(TriggerType.CRON, cronExpression);
    }

    public WorkflowBuilder webhookTrigger(String path) {
        return trigger(TriggerType.WEBHOOK, path);
    }

    public WorkflowBuilder eventTrigger(String eventName) {
        return trigger(TriggerType.EVENT, eventName);
    }

    public WorkflowBuilder manualTrigger() {
        return trigger(TriggerType.MANUAL, "");
    }

    public WorkflowBuilder fileWatchTrigger(String path) {
        return trigger(TriggerType.FILE_WATCH, path);
    }

    // --- Task methods ---

    /**
     * Adds a generic task with custom parameters.
     */
    public WorkflowBuilder addTask(String name, String type, Map<String, String> params) {
        tasks.add(new TaskConfig(name, type, params));
        return this;
    }

    /**
     * Convenience: add an HTTP task.
     */
    public WorkflowBuilder addHttpTask(String name, String url, String method) {
        return addTask(name, "http", Map.of("url", url, "method", method));
    }

    /**
     * Convenience: add an HTTP GET task.
     */
    public WorkflowBuilder addHttpGet(String name, String url) {
        return addHttpTask(name, url, "GET");
    }

    /**
     * Convenience: add an HTTP POST task.
     */
    public WorkflowBuilder addHttpPost(String name, String url) {
        return addHttpTask(name, url, "POST");
    }

    /**
     * Convenience: add a data transformation task.
     */
    public WorkflowBuilder addTransformTask(String name, String input, String operation) {
        return addTask(name, "transform", Map.of("input", input, "operation", operation));
    }

    /**
     * Convenience: add an email task.
     */
    public WorkflowBuilder addEmailTask(String name, String to, String subject) {
        return addTask(name, "email", Map.of("to", to, "subject", subject));
    }

    /**
     * Convenience: add a database task.
     */
    public WorkflowBuilder addDatabaseTask(String name, String query) {
        return addTask(name, "database", Map.of("query", query));
    }

    /**
     * Convenience: add a delay task.
     */
    public WorkflowBuilder addDelayTask(String name, int seconds) {
        return addTask(name, "delay", Map.of("seconds", String.valueOf(seconds)));
    }

    // --- Strategy ---

    public WorkflowBuilder sequential() {
        this.strategyName = "sequential";
        return this;
    }

    public WorkflowBuilder parallel() {
        this.strategyName = "parallel";
        return this;
    }

    public WorkflowBuilder strategy(String strategyName) {
        this.strategyName = strategyName;
        return this;
    }

    // --- Build ---

    /**
     * Validates and builds the WorkflowDefinition.
     *
     * @return immutable WorkflowDefinition
     * @throws IllegalStateException if required fields are missing
     */
    public WorkflowDefinition build() {
        List<String> errors = new ArrayList<>();

        if (name == null || name.isBlank()) {
            errors.add("Workflow name is required");
        }
        if (trigger == null) {
            errors.add("Trigger is required (use cronTrigger(), webhookTrigger(), etc.)");
        }
        if (tasks.isEmpty()) {
            errors.add("At least one task is required");
        }

        if (!errors.isEmpty()) {
            throw new IllegalStateException(
                    "Cannot build workflow — validation errors:\n  - "
                            + String.join("\n  - ", errors)
            );
        }

        return new WorkflowDefinition(name, trigger, tasks, strategyName);
    }
}