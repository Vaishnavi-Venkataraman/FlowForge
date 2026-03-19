package com.flowforge.pipeline;

import com.flowforge.model.TaskConfig;
import com.flowforge.model.WorkflowDefinition;
import com.flowforge.task.TaskFactory;

/**
 * Pipeline handler that validates the workflow definition before execution.
 * If validation fails, the pipeline is aborted — no subsequent handlers run.
 */
public class ValidationHandler implements PipelineHandler {

    private final TaskFactory taskFactory;

    public ValidationHandler(TaskFactory taskFactory) {
        this.taskFactory = taskFactory;
    }

    @Override
    public String getName() {
        return "ValidationHandler";
    }

    @Override
    public void handle(PipelineContext context, PipelineHandler next) {
        WorkflowDefinition workflow = context.getWorkflow();
        context.addLog("[ValidationHandler] Validating workflow: " + workflow.getName());

        // Check executable status
        if (!workflow.getStatus().isExecutable()) {
            context.abort("Workflow '" + workflow.getName()
                    + "' is not executable (status: " + workflow.getStatus() + ")");
            return;
        }

        // Check all task types are registered
        for (TaskConfig task : workflow.getTasks()) {
            if (!taskFactory.isTypeRegistered(task.getType())) {
                context.abort("Unknown task type '" + task.getType()
                        + "' in task '" + task.getName() + "'");
                return;
            }
        }

        // Check trigger exists
        if (workflow.getTrigger() == null) {
            context.abort("Workflow '" + workflow.getName() + "' has no trigger configured");
            return;
        }

        context.addLog("[ValidationHandler] Validation passed ("
                + workflow.getTasks().size() + " tasks, all types registered)");

        // Pass to next handler
        next.handle(context, null);
    }
}