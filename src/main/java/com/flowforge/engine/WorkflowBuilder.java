package com.flowforge.engine;

import com.flowforge.model.TaskConfig;
import com.flowforge.model.TriggerConfig;
import com.flowforge.model.TriggerType;
import com.flowforge.model.WorkflowDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Fluent builder for constructing WorkflowDefinition objects.
 */
public class WorkflowBuilder {

    private String existingId;
    private String name;
    private TriggerConfig trigger;
    private final List<TaskConfig> tasks = new ArrayList<>();
    private String strategyName = "sequential";

    private WorkflowBuilder() {}

    public static WorkflowBuilder create() {
        return new WorkflowBuilder();
    }

    public WorkflowBuilder name(String name) {
        this.name = name;
        return this;
    }

    /**
     * Sets a pre-existing ID (for reloading persisted workflows).
     * If not called, a new UUID is generated at build time.
     */
    public WorkflowBuilder withId(String id) {
        this.existingId = id;
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

    public WorkflowBuilder addTask(String name, String type, Map<String, String> params) {
        tasks.add(new TaskConfig(name, type, params));
        return this;
    }

    public WorkflowBuilder addHttpTask(String name, String url, String method) {
        return addTask(name, "http", Map.of("url", url, "method", method));
    }

    public WorkflowBuilder addHttpGet(String name, String url) {
        return addHttpTask(name, url, "GET");
    }

    public WorkflowBuilder addHttpPost(String name, String url) {
        return addHttpTask(name, url, "POST");
    }

    public WorkflowBuilder addTransformTask(String name, String input, String operation) {
        return addTask(name, "transform", Map.of("input", input, "operation", operation));
    }

    public WorkflowBuilder addEmailTask(String name, String to, String subject) {
        return addTask(name, "email", Map.of("to", to, "subject", subject));
    }

    public WorkflowBuilder addDatabaseTask(String name, String query) {
        return addTask(name, "database", Map.of("query", query));
    }

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

        return new WorkflowDefinition(existingId, name, trigger, tasks, strategyName);
    }
}