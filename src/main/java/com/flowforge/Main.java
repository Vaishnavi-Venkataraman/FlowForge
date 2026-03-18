package com.flowforge;

import java.util.ArrayList;
import java.util.HashMap;

public class Main {

    public static void main(String[] args) {
        WorkflowEngine engine = WorkflowEngine.getInstance();

        // Problem: building tasks with raw HashMaps — no type safety, easy to mistype keys
        ArrayList<HashMap<String, Object>> tasks = new ArrayList<>();

        HashMap<String, Object> task1 = new HashMap<>();
        task1.put("type", "http");
        task1.put("name", "Fetch User Data");
        task1.put("url", "https://api.example.com/users");
        task1.put("method", "GET");
        tasks.add(task1);

        HashMap<String, Object> task2 = new HashMap<>();
        task2.put("type", "transform");
        task2.put("name", "Filter Active Users");
        task2.put("input", "user_data");
        task2.put("operation", "filter_active");
        tasks.add(task2);

        HashMap<String, Object> task3 = new HashMap<>();
        task3.put("type", "email");
        task3.put("name", "Send Report");
        task3.put("to", "admin@company.com");
        task3.put("subject", "Daily Active Users Report");
        tasks.add(task3);

        String workflowId = engine.createWorkflow(
                "Daily User Report",
                "cron",
                "0 9 * * *",
                tasks
        );

        System.out.println("\n--- Executing Workflow ---\n");
        engine.executeWorkflow(workflowId);

        // Second workflow that fails (unknown task type)
        ArrayList<HashMap<String, Object>> tasks2 = new ArrayList<>();
        HashMap<String, Object> badTask = new HashMap<>();
        badTask.put("type", "ftp");
        badTask.put("name", "Upload to FTP");
        tasks2.add(badTask);

        String wfId2 = engine.createWorkflow(
                "FTP Upload",
                "webhook",
                "/api/trigger/upload",
                tasks2
        );

        System.out.println("\n--- Executing Failing Workflow ---\n");
        engine.executeWorkflow(wfId2);

        System.out.println("\n--- Statistics ---\n");
        engine.printStats();

        System.out.println("\n--- Logs ---");
        for (String log : engine.getLogs()) {
            System.out.println("  " + log);
        }
    }
}