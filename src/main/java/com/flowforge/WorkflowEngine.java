package com.flowforge;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

/**
 * NAIVE IMPLEMENTATION — Monolithic workflow engine.
 *
 * Known problems (intentional — will be fixed in later commits):
 * 1. God class: handles creation, execution, triggers, logging, notifications, stats
 * 2. HashMap for domain objects — zero type safety
 * 3. Hardcoded if-else chain for task types — violates Open/Closed Principle
 * 4. Tight coupling: logging, notification, metrics are inline
 * 5. Non-thread-safe singleton
 * 6. Public mutable fields
 * 7. Magic strings ("completed", "failed", "running")
 * 8. No error handling strategy — just prints and returns
 * 9. No interfaces or abstractions
 * 10. Linear search for workflow lookup
 */
public class WorkflowEngine {

    // Problem: Public mutable state — anyone can corrupt this
    public ArrayList<HashMap<String, Object>> workflows = new ArrayList<>();
    public ArrayList<String> logs = new ArrayList<>();
    public boolean running = false;

    // Problem: Non-thread-safe lazy singleton
    public static WorkflowEngine instance = null;

    public static WorkflowEngine getInstance() {
        if (instance == null) {
            instance = new WorkflowEngine();
        }
        return instance;
    }

    /**
     * Creates a workflow from raw parameters.
     * Problem: validation, storage, logging all mixed into one method.
     */
    public String createWorkflow(String name, String triggerType, String triggerValue,
                                  ArrayList<HashMap<String, Object>> tasks) {
        HashMap<String, Object> workflow = new HashMap<>();
        String id = "wf-" + System.currentTimeMillis();
        workflow.put("id", id);
        workflow.put("name", name);
        workflow.put("triggerType", triggerType);
        workflow.put("triggerValue", triggerValue);
        workflow.put("tasks", tasks);
        workflow.put("status", "created");
        workflow.put("createdAt", new Date().toString());

        if (name == null || name.equals("")) {
            System.out.println("ERROR: Workflow name is empty!");
            return null;
        }
        if (tasks == null || tasks.size() == 0) {
            System.out.println("ERROR: No tasks defined!");
            return null;
        }

        workflows.add(workflow);
        logs.add("Created workflow: " + name);
        System.out.println("LOG: Created workflow " + name + " with ID " + id);
        return id;
    }

    /**
     * Executes a workflow by ID.
     * Problem: massive method with if-else chain, inline notifications,
     * inline logging, no retry, no strategy selection.
     */
    @SuppressWarnings("unchecked")
    public void executeWorkflow(String workflowId) {
        HashMap<String, Object> workflow = null;

        // Problem: O(n) linear search every time
        for (HashMap<String, Object> wf : workflows) {
            if (wf.get("id").equals(workflowId)) {
                workflow = wf;
                break;
            }
        }

        if (workflow == null) {
            System.out.println("ERROR: Workflow not found: " + workflowId);
            return;
        }

        workflow.put("status", "running");
        logs.add("Started workflow: " + workflow.get("name"));
        System.out.println("LOG: Executing workflow " + workflow.get("name"));
        System.out.println("NOTIFICATION: Workflow " + workflow.get("name") + " started");

        ArrayList<HashMap<String, Object>> tasks =
                (ArrayList<HashMap<String, Object>>) workflow.get("tasks");

        for (HashMap<String, Object> task : tasks) {
            String taskType = (String) task.get("type");
            String taskName = (String) task.get("name");

            System.out.println("LOG: Executing task: " + taskName);
            logs.add("Executing task: " + taskName);

            // Problem: if-else chain — adding a new task type requires editing this method
            if (taskType.equals("http")) {
                String url = (String) task.get("url");
                String method = (String) task.get("method");
                System.out.println("  HTTP " + method + " " + url);
                task.put("result", "HTTP Response 200 OK");
                task.put("status", "completed");

            } else if (taskType.equals("transform")) {
                String input = (String) task.get("input");
                String operation = (String) task.get("operation");
                System.out.println("  Transform: " + operation + " on " + input);
                task.put("result", "Transformed: " + input);
                task.put("status", "completed");

            } else if (taskType.equals("email")) {
                String to = (String) task.get("to");
                String subject = (String) task.get("subject");
                System.out.println("  Sending email to " + to + ": " + subject);
                task.put("result", "Email sent");
                task.put("status", "completed");

            } else if (taskType.equals("database")) {
                String query = (String) task.get("query");
                System.out.println("  Executing query: " + query);
                task.put("result", "Query executed, 5 rows affected");
                task.put("status", "completed");

            } else if (taskType.equals("delay")) {
                int seconds = (int) task.get("seconds");
                System.out.println("  Waiting " + seconds + " seconds...");
                try {
                    Thread.sleep(seconds * 1000L);
                } catch (InterruptedException e) {
                    // Problem: swallowing exception
                }
                task.put("result", "Delay completed");
                task.put("status", "completed");

            } else {
                System.out.println("  ERROR: Unknown task type: " + taskType);
                task.put("status", "failed");
                task.put("error", "Unknown task type");
                workflow.put("status", "failed");
                logs.add("Workflow failed at task: " + taskName);
                System.out.println("NOTIFICATION: Workflow " + workflow.get("name") + " FAILED");
                return;
            }

            System.out.println("  Task " + taskName + " completed in 0ms");
        }

        workflow.put("status", "completed");
        logs.add("Workflow completed: " + workflow.get("name"));
        System.out.println("LOG: Workflow " + workflow.get("name") + " completed");
        System.out.println("NOTIFICATION: Workflow " + workflow.get("name") + " completed successfully");
    }

    /**
     * Checks all triggers — polling style, tightly coupled.
     */
    public void checkTriggers() {
        for (HashMap<String, Object> workflow : workflows) {
            String triggerType = (String) workflow.get("triggerType");
            String triggerValue = (String) workflow.get("triggerValue");
            String status = (String) workflow.get("status");

            if (status.equals("running")) {
                continue;
            }

            if (triggerType.equals("cron")) {
                System.out.println("Checking cron: " + triggerValue);
            } else if (triggerType.equals("webhook")) {
                System.out.println("Checking webhook: " + triggerValue);
            } else if (triggerType.equals("filewatch")) {
                System.out.println("Checking file: " + triggerValue);
            }
        }
    }

    /**
     * Returns logs — exposes internal mutable list directly.
     */
    public ArrayList<String> getLogs() {
        return logs;
    }

    /**
     * Prints statistics — reporting logic mixed into engine.
     */
    public void printStats() {
        int total = workflows.size();
        int completed = 0;
        int failed = 0;
        int created = 0;

        for (HashMap<String, Object> wf : workflows) {
            String status = (String) wf.get("status");
            if (status.equals("completed")) completed++;
            else if (status.equals("failed")) failed++;
            else if (status.equals("created")) created++;
        }

        System.out.println("=== Workflow Statistics ===");
        System.out.println("Total: " + total);
        System.out.println("Completed: " + completed);
        System.out.println("Failed: " + failed);
        System.out.println("Pending: " + created);
    }
} 
