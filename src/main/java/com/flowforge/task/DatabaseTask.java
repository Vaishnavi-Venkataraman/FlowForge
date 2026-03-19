package com.flowforge.task;

import com.flowforge.model.TaskConfig;
import com.flowforge.model.TaskResult;

import java.time.Instant;

/**
 * Simulates a database query execution.
 */
public class DatabaseTask implements Task {

    private final String name;

    public DatabaseTask(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getType() {
        return "database";
    }

    @Override
    public TaskResult execute(TaskConfig config) {
        Instant start = Instant.now();
        String query = config.getRequiredParameter("query");
        System.out.println("  Executing query: " + query);
        return TaskResult.success("Query executed, 5 rows affected", start);
    }
}