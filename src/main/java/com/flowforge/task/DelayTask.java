package com.flowforge.task;

import com.flowforge.model.TaskConfig;
import com.flowforge.model.TaskResult;

import java.time.Instant;

/**
 * Introduces a configurable delay into the workflow pipeline.
 */
public class DelayTask implements Task {

    private final String name;

    public DelayTask(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getType() {
        return "delay";
    }

    @Override
    public TaskResult execute(TaskConfig config) {
        Instant start = Instant.now();
        int seconds = Integer.parseInt(config.getParameter("seconds", "1"));
        System.out.println("  Waiting " + seconds + " seconds...");
        try {
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return TaskResult.failure("Delay interrupted", start);
        }
        return TaskResult.success("Delay of " + seconds + "s completed", start);
    }
}