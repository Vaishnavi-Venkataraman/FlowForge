package com.flowforge.engine;

import com.flowforge.engine.strategy.ExecutionStrategy;
import com.flowforge.engine.strategy.SequentialStrategy;
import com.flowforge.event.EventBus;
import com.flowforge.event.WorkflowEvent;
import com.flowforge.exception.TaskExecutionException;
import com.flowforge.exception.WorkflowNotFoundException;
import com.flowforge.model.TaskResult;
import com.flowforge.model.WorkflowDefinition;
import com.flowforge.model.WorkflowStatus;
import com.flowforge.pipeline.Pipeline;
import com.flowforge.pipeline.PipelineContext;
import com.flowforge.task.TaskFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * ARCHITECTURAL PATTERNS:
 * - Pipes & Filters: data (PipelineContext) flows through handlers
 * - Chain of Responsibility: each handler decides to process or abort
 * - Event-Driven: events published for all lifecycle transitions
 */
public class WorkflowEngine {

    private final Map<String, WorkflowDefinition> workflows = new HashMap<>();
    private final Map<String, ExecutionStrategy> strategies = new HashMap<>();
    private final TaskFactory taskFactory;
    private final EventBus eventBus;
    private Pipeline pipeline;

    public WorkflowEngine(TaskFactory taskFactory, EventBus eventBus) {
        this.taskFactory = taskFactory;
        this.eventBus = eventBus;
        registerStrategy(new SequentialStrategy());
    }

    /**
     * Sets the pre-execution pipeline.
     * If null, workflows execute directly without pre-processing.
     */
    public void setPipeline(Pipeline pipeline) {
        this.pipeline = pipeline;
    }

    public void registerStrategy(ExecutionStrategy strategy) {
        strategies.put(strategy.getName(), strategy);
    }

    public String registerWorkflow(WorkflowDefinition workflow) {
        workflows.put(workflow.getId(), workflow);
        eventBus.publish(WorkflowEvent.workflowRegistered(workflow.getName(), workflow.getId()));
        return workflow.getId();
    }

    public void executeWorkflow(String workflowId) {
        WorkflowDefinition workflow = workflows.get(workflowId);
        if (workflow == null) {
            throw new WorkflowNotFoundException(workflowId);
        }

        if (!workflow.getStatus().isExecutable()) {
            return;
        }

        // --- Run through pipeline (Chain of Responsibility) ---
        PipelineContext pipelineResult = null;
        if (pipeline != null) {
            PipelineContext context = new PipelineContext(workflow);
            pipelineResult = pipeline.execute(context);

            // Print pipeline processing log
            for (String log : pipelineResult.getProcessingLog()) {
                System.out.println("  [Pipeline] " + log);
            }

            // If pipeline aborted, do not execute
            if (pipelineResult.isAborted()) {
                workflow.setStatus(WorkflowStatus.FAILED);
                eventBus.publish(WorkflowEvent.workflowFailed(
                        workflow.getName(), workflow.getId(),
                        "Pipeline aborted: " + pipelineResult.getAbortReason()));
                return;
            }
        }

        // --- Execute with strategy ---
        ExecutionStrategy strategy = resolveStrategy(workflow.getExecutionStrategyName());
        workflow.setStatus(WorkflowStatus.RUNNING);
        eventBus.publish(WorkflowEvent.workflowStarted(
                workflow.getName(), workflow.getId(), strategy.getName()));

        try {
            // Use pipeline-processed tasks if available, otherwise original
            var tasksToExecute = (pipelineResult != null)
                    ? pipelineResult.getProcessedTasks()
                    : workflow.getTasks();

            Map<String, TaskResult> results = strategy.execute(tasksToExecute, taskFactory);

            for (Map.Entry<String, TaskResult> entry : results.entrySet()) {
                TaskResult result = entry.getValue();
                if (result.isSuccess()) {
                    eventBus.publish(WorkflowEvent.taskCompleted(
                            workflow.getName(), workflow.getId(),
                            entry.getKey(), result.getDuration().toMillis()));
                }
            }

            workflow.setStatus(WorkflowStatus.COMPLETED);
            eventBus.publish(WorkflowEvent.workflowCompleted(
                    workflow.getName(), workflow.getId(), results.size()));

        } catch (TaskExecutionException e) {
            workflow.setStatus(WorkflowStatus.FAILED);
            eventBus.publish(WorkflowEvent.workflowFailed(
                    workflow.getName(), workflow.getId(), e.getMessage()));
            throw e;
        }
    }

    private ExecutionStrategy resolveStrategy(String name) {
        ExecutionStrategy strategy = strategies.get(name);
        if (strategy != null) {
            return strategy;
        }
        for (Map.Entry<String, ExecutionStrategy> entry : strategies.entrySet()) {
            if (entry.getKey().startsWith(name)) {
                return entry.getValue();
            }
        }
        return strategies.getOrDefault("sequential", new SequentialStrategy());
    }

    public TaskFactory getTaskFactory() {
        return taskFactory;
    }

    public EventBus getEventBus() {
        return eventBus;
    }
}