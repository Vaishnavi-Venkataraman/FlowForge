package com.flowforge.engine;

import com.flowforge.exception.TaskExecutionException;
import com.flowforge.exception.WorkflowNotFoundException;
import com.flowforge.model.TaskConfig;
import com.flowforge.model.TaskResult;
import com.flowforge.model.WorkflowDefinition;
import com.flowforge.model.WorkflowStatus;
import com.flowforge.task.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Workflow execution engine — refactored to use domain models and Task interface.
 *
 * IMPROVEMENTS OVER COMMIT 1:
 * - Uses typed domain models instead of HashMap<String, Object>
 * - Task interface replaces the if-else chain (OCP)
 * - Proper exception handling instead of println + return null
 * - Workflow lookup via HashMap (O(1)) instead of linear scan
 * - Separated logs list from direct console output
 *
 * REMAINING PROBLEMS (to fix in later commits):
 * - Still has inline task creation (needs Factory pattern — Commit 3)
 * - Only sequential execution (needs Strategy pattern — Commit 4)
 * - Logging/notification still inline (needs Observer pattern — Commit 5)
 * - Workflow creation is verbose (needs Builder pattern — Commit 6)
 */
public class WorkflowEngine {

    private final Map<String, WorkflowDefinition> workflows = new HashMap<>();
    private final List<String> logs = new ArrayList<>();

    /**
     * Registers a workflow definition.
     *
     * @param workflow the workflow to register
     * @return the workflow ID
     */
    public String registerWorkflow(WorkflowDefinition workflow) {
        workflows.put(workflow.getId(), workflow);
        log("Registered workflow: " + workflow.getName() + " [" + workflow.getId() + "]");
        return workflow.getId();
    }

    /**
     * Executes a workflow by ID.
     *
     * @param workflowId the ID of the workflow to execute
     * @throws WorkflowNotFoundException if the ID is not found
     * @throws TaskExecutionException    if a task fails
     */
    public void executeWorkflow(String workflowId) {
        WorkflowDefinition workflow = workflows.get(workflowId);
        if (workflow == null) {
            throw new WorkflowNotFoundException(workflowId);
        }

        if (!workflow.getStatus().isExecutable()) {
            log("Workflow " + workflow.getName() + " is not executable (status: " + workflow.getStatus() + ")");
            return;
        }

        workflow.setStatus(WorkflowStatus.RUNNING);
        log("Started workflow: " + workflow.getName());

        // STILL A PROBLEM: notification is inline here — will fix with Observer in Commit 5
        System.out.println("NOTIFICATION: Workflow " + workflow.getName() + " started");

        try {
            for (TaskConfig taskConfig : workflow.getTasks()) {
                Task task = createTask(taskConfig);
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
     * Creates a Task from config.
     *
     * PROBLEM: This is still a crude switch — will be replaced by Factory pattern in Commit 3.
     * But it's already better than the original because new tasks only need a new case here,
     * not changes to the execution logic.
     */
    private Task createTask(TaskConfig config) {
        return switch (config.getType()) {
            case "http" -> new HttpTask(config.getName());
            case "email" -> new EmailTask(config.getName());
            case "transform" -> new TransformTask(config.getName());
            case "database" -> new DatabaseTask(config.getName());
            case "delay" -> new DelayTask(config.getName());
            default -> throw new TaskExecutionException(
                    config.getName(), "Unknown task type: " + config.getType());
        };
    }

    /**
     * Returns an unmodifiable copy of logs.
     */
    public List<String> getLogs() {
        return List.copyOf(logs);
    }

    /**
     * Prints workflow statistics.
     *
     * PROBLEM: reporting logic still lives in the engine — will extract later.
     */
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