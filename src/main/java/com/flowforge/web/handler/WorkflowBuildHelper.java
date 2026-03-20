package com.flowforge.web.handler;

import com.flowforge.engine.WorkflowBuilder;
import com.flowforge.model.WorkflowDefinition;
import com.flowforge.web.WorkflowStore.TaskInfo;

import java.util.List;

/**
 * Shared helper for building WorkflowDefinition from stored/submitted data.
 * Extracted to eliminate duplication between createWorkflow and reloadPersistedWorkflows.
 */
public final class WorkflowBuildHelper {

    private WorkflowBuildHelper() {}

    /**
     * Builds a NEW workflow (new ID generated).
     */
    public static WorkflowDefinition buildWorkflow(String name, String triggerType, String triggerValue,
                                                    String strategy, List<TaskInfo> tasks) {
        return buildWorkflow(null, name, triggerType, triggerValue, strategy, tasks);
    }

    /**
     * Builds a workflow with a specific ID (for reloading from persistence).
     * If existingId is null, a new UUID is generated.
     */
    public static WorkflowDefinition buildWorkflow(String existingId, String name, String triggerType,
                                                    String triggerValue, String strategy, List<TaskInfo> tasks) {
        WorkflowBuilder builder = WorkflowBuilder.create().name(name);

        if (existingId != null) {
            builder.withId(existingId);
        }

        switch (triggerType.toUpperCase()) {
            case "CRON" -> builder.cronTrigger(triggerValue);
            case "WEBHOOK" -> builder.webhookTrigger(triggerValue);
            case "EVENT" -> builder.eventTrigger(triggerValue);
            case "FILE_WATCH" -> builder.fileWatchTrigger(triggerValue);
            default -> builder.manualTrigger();
        }

        for (TaskInfo ti : tasks) {
            builder.addTask(ti.name(), ti.type(), ti.params());
        }

        builder.strategy(strategy);
        return builder.build();
    }
}