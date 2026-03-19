package com.flowforge.task;

import com.flowforge.model.TaskConfig;
import com.flowforge.model.TaskResult;

import java.time.Instant;

/**
 * Simulates an HTTP API call.
 *
 * WHY: Extracted from the if(taskType.equals("http")) block in the
 * naive engine. Each task type is now its own class with focused logic.
 */
public class HttpTask implements Task {

    private final String name;

    public HttpTask(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getType() {
        return "http";
    }

    @Override
    public TaskResult execute(TaskConfig config) {
        Instant start = Instant.now();
        String url = config.getRequiredParameter("url");
        String method = config.getParameter("method", "GET");
        System.out.println("  HTTP " + method + " " + url);
        return TaskResult.success("HTTP Response 200 OK from " + url, start);
    }
}