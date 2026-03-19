package com.flowforge.engine;

import com.flowforge.engine.strategy.ExecutionStrategy;
import com.flowforge.engine.strategy.SequentialStrategy;
import com.flowforge.exception.TaskExecutionException;
import com.flowforge.exception.WorkflowNotFoundException;
import com.flowforge.model.TaskResult;
import com.flowforge.model.WorkflowDefinition;
import com.flowforge.model.WorkflowStatus;
import com.flowforge.task.TaskFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class WorkflowEngine {

    private final Map<String, WorkflowDefinition> workflows = new HashMap<>();
    private final Map<String, ExecutionStrategy> strategies = new HashMap<>();
    private final List<String> logs = new ArrayList<>();
    private final TaskFactory taskFactory;

    public WorkflowEngine(TaskFactory taskFactory) {
        this.taskFactory = taskFactory;
        // Register default strategy
        registerStrategy(new SequentialStrategy());
    }

    /**
     * Registers an execution strategy by name.
     * This is the extension point for new execution models.
     */
    public void registerStrategy(ExecutionStrategy strategy) {
        strategies.put(strategy.getName(), strategy);
        log("Registered execution strategy: " + strategy.getName());
    }

    public String registerWorkflow(WorkflowDefinition workflow) {
        workflows.put(workflow.getId(), workflow);
        log("Registered workflow: " + workflow.getName()
                + " [strategy=" + workflow.getExecutionStrategyName() + "]");
        return workflow.getId();
    }

    public void executeWorkflow(String workflowId) {
        WorkflowDefinition workflow = workflows.get(workflowId);
        if (workflow == null) {
            throw new WorkflowNotFoundException(workflowId);
        }

        if (!workflow.getStatus().isExecutable()) {
            log("Workflow " + workflow.getName() + " not executable (status: "
                    + workflow.getStatus() + ")");
            return;
        }

        // Resolve the strategy for this workflow
        ExecutionStrategy strategy = resolveStrategy(workflow.getExecutionStrategyName());

        workflow.setStatus(WorkflowStatus.RUNNING);
        log("Started workflow: " + workflow.getName() + " using strategy: " + strategy.getName());
        System.out.println("NOTIFICATION: Workflow " + workflow.getName() + " started");

        try {
            // DELEGATE execution to the strategy — engine doesn't loop over tasks anymore
            Map<String, TaskResult> results = strategy.execute(
                    workflow.getTasks(), taskFactory
            );

            workflow.setStatus(WorkflowStatus.COMPLETED);
            log("Workflow completed: " + workflow.getName()
                    + " (" + results.size() + " tasks executed)");
            System.out.println("NOTIFICATION: Workflow " + workflow.getName() + " completed");

        } catch (TaskExecutionException e) {
            workflow.setStatus(WorkflowStatus.FAILED);
            log("Workflow FAILED: " + workflow.getName() + " — " + e.getMessage());
            System.out.println("NOTIFICATION: Workflow " + workflow.getName() + " FAILED");
            throw e;
        }
    }

    /**
     * Resolves a strategy by name. Falls back to sequential if not found.
     */
    private ExecutionStrategy resolveStrategy(String name) {
        // Try exact match first
        ExecutionStrategy strategy = strategies.get(name);
        if (strategy != null) {
            return strategy;
        }

        // For strategies with parameters like "conditional(...)", check prefix
        for (Map.Entry<String, ExecutionStrategy> entry : strategies.entrySet()) {
            if (entry.getKey().startsWith(name)) {
                return entry.getValue();
            }
        }

        log("Strategy '" + name + "' not found, falling back to sequential");
        return strategies.getOrDefault("sequential", new SequentialStrategy());
    }

    public TaskFactory getTaskFactory() {
        return taskFactory;
    }

    public List<String> getLogs() {
        return List.copyOf(logs);
    }

    public void printStats() {
        long total = workflows.size();
        long completed = workflows.values().stream()
                .filter(w -> w.getStatus() == WorkflowStatus.COMPLETED).count();
        long failed = workflows.values().stream()
                .filter(w -> w.getStatus() == WorkflowStatus.FAILED).count();
        long pending = workflows.values().stream()
                .filter(w -> w.getStatus() == WorkflowStatus.CREATED).count();

        System.out.println("=== Workflow Statistics ===");
        System.out.println("Total: " + total);
        System.out.println("Completed: " + completed);
        System.out.println("Failed: " + failed);
        System.out.println("Pending: " + pending);
    }

    private void log(String message) {
        logs.add(message);
        System.out.println("LOG: " + message);
    }
}