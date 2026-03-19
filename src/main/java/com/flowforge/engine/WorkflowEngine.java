package com.flowforge.engine;

import com.flowforge.engine.strategy.ExecutionStrategy;
import com.flowforge.engine.strategy.SequentialStrategy;
import com.flowforge.event.EventBus;
import com.flowforge.event.WorkflowEvent;
import com.flowforge.exception.TaskExecutionException;
import com.flowforge.exception.WorkflowNotFoundException;
import com.flowforge.model.TaskConfig;
import com.flowforge.model.TaskResult;
import com.flowforge.model.WorkflowDefinition;
import com.flowforge.pipeline.Pipeline;
import com.flowforge.pipeline.PipelineContext;
import com.flowforge.task.TaskFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WorkflowEngine {

    private final Map<String, WorkflowDefinition> workflows = new ConcurrentHashMap<>();
    private final Map<String, ExecutionStrategy> strategies = new ConcurrentHashMap<>();
    private final TaskFactory taskFactory;
    private final EventBus eventBus;
    private volatile Pipeline pipeline;

    public WorkflowEngine(TaskFactory taskFactory, EventBus eventBus) {
        this.taskFactory = taskFactory;
        this.eventBus = eventBus;
        registerStrategy(new SequentialStrategy());
    }

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

    /**
     * Main execution method — orchestrates pipeline + strategy.
     * Each phase is extracted into a focused helper method.
     */
    public void executeWorkflow(String workflowId) {
        WorkflowDefinition workflow = findWorkflow(workflowId);

        if (!workflow.getStatus().isExecutable()) {
            return;
        }

        List<TaskConfig> tasksToExecute = runPipeline(workflow);
        if (tasksToExecute == null) {
            return; // Pipeline aborted
        }

        runWithStrategy(workflow, tasksToExecute);
    }

    /**
     * Looks up a workflow by ID.
     * @throws WorkflowNotFoundException if not found
     */
    private WorkflowDefinition findWorkflow(String workflowId) {
        WorkflowDefinition workflow = workflows.get(workflowId);
        if (workflow == null) {
            throw new WorkflowNotFoundException(workflowId);
        }
        return workflow;
    }

    /**
     * Runs the pre-execution pipeline. Returns the (possibly enriched) task list,
     * or null if the pipeline aborted.
     */
    private List<TaskConfig> runPipeline(WorkflowDefinition workflow) {
        if (pipeline == null) {
            return workflow.getTasks();
        }

        PipelineContext context = new PipelineContext(workflow);
        PipelineContext result = pipeline.execute(context);

        for (String log : result.getProcessingLog()) {
            eventBus.publish(WorkflowEvent.pipelineLog(workflow.getName(), workflow.getId(), log));
        }

        if (result.isAborted()) {
            workflow.markFailed();
            eventBus.publish(WorkflowEvent.workflowFailed(
                    workflow.getName(), workflow.getId(),
                    "Pipeline aborted: " + result.getAbortReason()));
            return null;
        }

        return result.getProcessedTasks();
    }

    /**
     * Executes tasks using the workflow's chosen strategy.
     */
    private void runWithStrategy(WorkflowDefinition workflow, List<TaskConfig> tasks) {
        ExecutionStrategy strategy = resolveStrategy(workflow.getExecutionStrategyName());

        workflow.markRunning();
        eventBus.publish(WorkflowEvent.workflowStarted(
                workflow.getName(), workflow.getId(), strategy.getName()));

        try {
            Map<String, TaskResult> results = strategy.execute(tasks, taskFactory);
            publishTaskResults(workflow, results);

            workflow.markCompleted();
            eventBus.publish(WorkflowEvent.workflowCompleted(
                    workflow.getName(), workflow.getId(), results.size()));

        } catch (TaskExecutionException e) {
            workflow.markFailed();
            eventBus.publish(WorkflowEvent.workflowFailed(
                    workflow.getName(), workflow.getId(), e.getMessage()));
            throw e;
        }
    }

    private void publishTaskResults(WorkflowDefinition workflow, Map<String, TaskResult> results) {
        for (Map.Entry<String, TaskResult> entry : results.entrySet()) {
            if (entry.getValue().isSuccess()) {
                eventBus.publish(WorkflowEvent.taskCompleted(
                        workflow.getName(), workflow.getId(),
                        entry.getKey(), entry.getValue().getDuration().toMillis()));
            }
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