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
import com.flowforge.task.TaskFactory;

import java.util.HashMap;
import java.util.Map;

/* ARCHITECTURAL PATTERN: Event-Driven Architecture */
public class WorkflowEngine {

    private final Map<String, WorkflowDefinition> workflows = new HashMap<>();
    private final Map<String, ExecutionStrategy> strategies = new HashMap<>();
    private final TaskFactory taskFactory;
    private final EventBus eventBus;

    public WorkflowEngine(TaskFactory taskFactory, EventBus eventBus) {
        this.taskFactory = taskFactory;
        this.eventBus = eventBus;
        registerStrategy(new SequentialStrategy());
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

        ExecutionStrategy strategy = resolveStrategy(workflow.getExecutionStrategyName());

        workflow.setStatus(WorkflowStatus.RUNNING);
        eventBus.publish(WorkflowEvent.workflowStarted(
                workflow.getName(), workflow.getId(), strategy.getName()));

        try {
            Map<String, TaskResult> results = strategy.execute(
                    workflow.getTasks(), taskFactory
            );

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