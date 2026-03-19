package com.flowforge.task;

import com.flowforge.model.TaskConfig;
import com.flowforge.model.TaskResult;

import java.time.Instant;

/**
 * Simulates a data transformation operation.
 */
public class TransformTask implements Task {

    private final String name;

    public TransformTask(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getType() {
        return "transform";
    }

    @Override
    public TaskResult execute(TaskConfig config) {
        Instant start = Instant.now();
        String input = config.getRequiredParameter("input");
        String operation = config.getParameter("operation", "identity");
        System.out.println("  Transform: " + operation + " on " + input);
        return TaskResult.success("Transformed(" + operation + "): " + input, start);
    }
}