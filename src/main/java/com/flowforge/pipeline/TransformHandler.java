package com.flowforge.pipeline;

import com.flowforge.model.TaskConfig;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * This handler modifies the task list in the context — downstream handlers (including execution) see the enriched configs.
 * This is a classic "filter" in the Pipes & Filters pattern:data flows in, gets transformed, flows out.
 */ 
public class TransformHandler implements PipelineHandler {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public String getName() {
        return "TransformHandler";
    }

    @Override
    public void handle(PipelineContext context, PipelineHandler next) {
        context.addLog("[TransformHandler] Enriching task configurations");

        String workflowName = context.getWorkflow().getName();
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        int enrichedCount = 0;

        List<TaskConfig> enrichedTasks = new ArrayList<>();

        for (TaskConfig original : context.getProcessedTasks()) {
            Map<String, String> enrichedParams = new HashMap<>(original.getParameters());
            boolean modified = false;

            // Replace placeholders in all parameter values
            for (Map.Entry<String, String> entry : enrichedParams.entrySet()) {
                String value = entry.getValue();
                if (value.contains("${timestamp}")) {
                    entry.setValue(value.replace("${timestamp}", timestamp));
                    modified = true;
                }
                if (value.contains("${workflow.name}")) {
                    entry.setValue(value.replace("${workflow.name}", workflowName));
                    modified = true;
                }
            }

            if (modified) {
                enrichedTasks.add(new TaskConfig(original.getName(), original.getType(), enrichedParams));
                enrichedCount++;
            } else {
                enrichedTasks.add(original);
            }
        }

        context.setProcessedTasks(enrichedTasks);
        context.addLog("[TransformHandler] Enriched " + enrichedCount + " task(s)");

        next.handle(context, null);
    }
}