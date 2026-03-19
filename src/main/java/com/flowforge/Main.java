package com.flowforge;

import com.flowforge.engine.WorkflowEngine;
import com.flowforge.exception.TaskExecutionException;
import com.flowforge.model.TaskConfig;
import com.flowforge.model.TriggerConfig;
import com.flowforge.model.TriggerType;
import com.flowforge.model.WorkflowDefinition;

import java.util.List;
import java.util.Map;

/**
 * Entry point — demonstrates the refactored engine with domain models.
 *
 * Compare with Commit 1: no more raw HashMaps, typed configs, proper exceptions.
 */
public class Main {

    public static void main(String[] args) {
        WorkflowEngine engine = new WorkflowEngine();

        // --- Workflow 1: Daily User Report (succeeds) ---
        TaskConfig fetchUsers = new TaskConfig("Fetch User Data", "http", Map.of(
                "url", "https://api.example.com/users",
                "method", "GET"
        ));
        TaskConfig filterActive = new TaskConfig("Filter Active Users", "transform", Map.of(
                "input", "user_data",
                "operation", "filter_active"
        ));
        TaskConfig sendReport = new TaskConfig("Send Report", "email", Map.of(
                "to", "admin@company.com",
                "subject", "Daily Active Users Report"
        ));

        WorkflowDefinition dailyReport = new WorkflowDefinition(
                "Daily User Report",
                new TriggerConfig(TriggerType.CRON, "0 9 * * *"),
                List.of(fetchUsers, filterActive, sendReport)
        );

        String wfId1 = engine.registerWorkflow(dailyReport);

        System.out.println("\n--- Executing Workflow 1 ---\n");
        engine.executeWorkflow(wfId1);

        // --- Workflow 2: Unknown task type (fails with proper exception) ---
        TaskConfig badTask = new TaskConfig("Upload to FTP", "ftp", Map.of(
                "host", "ftp.example.com"
        ));

        WorkflowDefinition ftpUpload = new WorkflowDefinition(
                "FTP Upload",
                new TriggerConfig(TriggerType.WEBHOOK, "/api/trigger/upload"),
                List.of(badTask)
        );

        String wfId2 = engine.registerWorkflow(ftpUpload);

        System.out.println("\n--- Executing Workflow 2 (will fail) ---\n");
        try {
            engine.executeWorkflow(wfId2);
        } catch (TaskExecutionException e) {
            System.out.println("Caught expected exception: " + e.getMessage());
        }

        // --- Stats and Logs ---
        System.out.println("\n--- Statistics ---\n");
        engine.printStats();

        System.out.println("\n--- Logs ---");
        for (String log : engine.getLogs()) {
            System.out.println("  " + log);
        }
    }
}