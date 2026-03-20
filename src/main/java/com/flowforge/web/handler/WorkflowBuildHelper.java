package com.flowforge.web.handler;

import com.flowforge.engine.WorkflowBuilder;
import com.flowforge.model.WorkflowDefinition;
import com.flowforge.web.WorkflowStore.TaskInfo;

import java.util.List;

public final class WorkflowBuildHelper {

    private WorkflowBuildHelper() {}

    /**
     * Builds a WorkflowDefinition from component data.
     */
    public static WorkflowDefinition buildWorkflow(String name, String triggerType, String triggerValue,
                                                    String strategy, List<TaskInfo> tasks) {
        WorkflowBuilder builder = WorkflowBuilder.create().name(name);

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