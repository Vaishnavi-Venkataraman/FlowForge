package com.flowforge;

import com.flowforge.engine.WorkflowEngine;
import com.flowforge.exception.TaskExecutionException;
import com.flowforge.model.TaskConfig;
import com.flowforge.model.TaskResult;
import com.flowforge.model.TriggerConfig;
import com.flowforge.model.TriggerType;
import com.flowforge.model.WorkflowDefinition;
import com.flowforge.task.Task;
import com.flowforge.task.TaskFactory;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Entry point — demonstrates Factory pattern for task creation.
 *
 * Key demo: registering a custom FTP task type at runtime without
 * modifying the engine or any existing code.
 */
public class Main {

    public static void main(String[] args) {
        // Create factory with built-in tasks, then register a custom one
        TaskFactory factory = new TaskFactory();

        // DEMO: Register custom "ftp" task type at runtime
        // In Commit 1, "ftp" would cause a failure. Now we can add it dynamically.
        factory.registerTaskType("ftp", name -> new Task() {
            @Override
            public String getName() { return name; }

            @Override
            public String getType() { return "ftp"; }

            @Override
            public TaskResult execute(TaskConfig config) {
                Instant start = Instant.now();
                String host = config.getRequiredParameter("host");
                String file = config.getParameter("file", "data.csv");
                System.out.println("  FTP upload " + file + " to " + host);
                return TaskResult.success("Uploaded " + file + " to " + host, start);
            }
        });

        WorkflowEngine engine = new WorkflowEngine(factory);

        // --- Workflow 1: Daily Report (succeeds) ---
        WorkflowDefinition dailyReport = new WorkflowDefinition(
                "Daily User Report",
                new TriggerConfig(TriggerType.CRON, "0 9 * * *"),
                List.of(
                        new TaskConfig("Fetch User Data", "http", Map.of(
                                "url", "https://api.example.com/users", "method", "GET")),
                        new TaskConfig("Filter Active Users", "transform", Map.of(
                                "input", "user_data", "operation", "filter_active")),
                        new TaskConfig("Send Report", "email", Map.of(
                                "to", "admin@company.com", "subject", "Daily Active Users Report"))
                )
        );

        String wfId1 = engine.registerWorkflow(dailyReport);
        System.out.println("\n--- Executing Workflow 1: Daily Report ---\n");
        engine.executeWorkflow(wfId1);

        // --- Workflow 2: FTP Upload (NOW succeeds thanks to custom registration) ---
        WorkflowDefinition ftpWorkflow = new WorkflowDefinition(
                "FTP Data Export",
                new TriggerConfig(TriggerType.WEBHOOK, "/api/trigger/export"),
                List.of(
                        new TaskConfig("Query Sales Data", "database", Map.of(
                                "query", "SELECT * FROM sales WHERE date = TODAY")),
                        new TaskConfig("Upload to FTP", "ftp", Map.of(
                                "host", "ftp.partner.com", "file", "sales_export.csv"))
                )
        );

        String wfId2 = engine.registerWorkflow(ftpWorkflow);
        System.out.println("\n--- Executing Workflow 2: FTP Export ---\n");
        engine.executeWorkflow(wfId2);

        // --- Workflow 3: Unknown type still fails cleanly ---
        WorkflowDefinition badWorkflow = new WorkflowDefinition(
                "Bad Workflow",
                new TriggerConfig(TriggerType.MANUAL, ""),
                List.of(new TaskConfig("Mystery Task", "unknown_type", Map.of()))
        );

        String wfId3 = engine.registerWorkflow(badWorkflow);
        System.out.println("\n--- Executing Workflow 3: Unknown Type ---\n");
        try {
            engine.executeWorkflow(wfId3);
        } catch (TaskExecutionException e) {
            System.out.println("Caught: " + e.getMessage());
        }

        // --- Stats ---
        System.out.println("\n--- Statistics ---\n");
        engine.printStats();
    }
}