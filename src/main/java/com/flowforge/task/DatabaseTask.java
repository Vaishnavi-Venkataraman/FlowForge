package com.flowforge.task;

import com.flowforge.model.TaskConfig;

/**
 * Database task — refactored with Template Method lifecycle.
 *
 * Demonstrates the cleanup() hook: in a real implementation,
 * cleanup() would close database connections / release pool resources.
 */
public class DatabaseTask extends AbstractTask {

    public DatabaseTask(String name) {
        super(name);
    }

    @Override
    public String getType() {
        return "database";
    }

    @Override
    protected void validate(TaskConfig config) {
        config.getRequiredParameter("query");
    }

    @Override
    protected String doExecute(TaskConfig config) {
        String query = config.getRequiredParameter("query");
        System.out.println("    Executing query: " + query);
        return "Query executed, 5 rows affected";
    }

    @Override
    protected void cleanup(TaskConfig config) {
        // In a real system: close connection, return to pool
        System.out.println("    [DatabaseTask] Connection returned to pool");
    }
}