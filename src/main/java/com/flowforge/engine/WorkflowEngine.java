package com.flowforge.engine;

import com.flowforge.exception.TaskExecutionException;
import com.flowforge.exception.WorkflowNotFoundException;
import com.flowforge.model.TaskConfig;
import com.flowforge.model.TaskResult;
import com.flowforge.model.WorkflowDefinition;
import com.flowforge.model.WorkflowStatus;
import com.flowforge.task.Task;
import com.flowforge.task.TaskFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorkflowEngine {

    private final Map<String, WorkflowDefinition> workflows = new HashMap<>();
    private final List<String> logs = new ArrayList<>();
    private final TaskFactory taskFactory;

    /**
     * Engine now receives its TaskFactory — a step toward dependency injection.
     */
    public WorkflowEngine(TaskFactory taskFactory) {
        this.taskFactory = taskFactory;
    }

    public String registerWorkflow(WorkflowDefinition workflow) {
        workflows.put(workflow.getId(), workflow);
        log("Registered workflow: " + workflow.getName() + " [" + workflow.getId() + "]");
        return workflow.getId();
    }

    public void executeWorkflow(String workflowId) {
        WorkflowDefinition workflow = workflows.get(workflowId);
        if (workflow == null) {
            throw new WorkflowNotFoundException(workflowId);
        }

        if (!workflow.getStatus().isExecutable()) {
            log("Workflow " + workflow.getName() + " not executable (status: " + workflow.getStatus() + ")");
            return;
        }

        workflow.setStatus(WorkflowStatus.RUNNING);
        log("Started workflow: " + workflow.getName());
        System.out.println("NOTIFICATION: Workflow " + workflow.getName() + " started");

        try {
            for (TaskConfig taskConfig : workflow.getTasks()) {
                // Factory creates the task — engine doesn't know concrete types
                Task task = taskFactory.createTask(taskConfig);
                log("Executing task: " + task.getName() + " [" + task.getType() + "]");

                TaskResult result = task.execute(taskConfig);

                if (!result.isSuccess()) {
                    throw new TaskExecutionException(task.getName(), result.getErrorMessage());
                }

                log("  Task completed: " + result);
            }

            workflow.setStatus(WorkflowStatus.COMPLETED);
            log("Workflow completed: " + workflow.getName());
            System.out.println("NOTIFICATION: Workflow " + workflow.getName() + " completed");

        } catch (TaskExecutionException e) {
            workflow.setStatus(WorkflowStatus.FAILED);
            log("Workflow FAILED: " + workflow.getName() + " — " + e.getMessage());
            System.out.println("NOTIFICATION: Workflow " + workflow.getName() + " FAILED");
            throw e;
        }
    }

    /**
     * Provides access to the factory so callers can register custom task types.
     */
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