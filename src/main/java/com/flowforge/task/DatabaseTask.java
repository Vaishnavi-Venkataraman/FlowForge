package com.flowforge.task;
import java.util.logging.Logger;
import com.flowforge.model.TaskConfig;

/**
 * Database task — refactored with Template Method lifecycle.
 *
 * Demonstrates the cleanup() hook: in a real implementation,
 * cleanup() would close database connections / release pool resources.
 */
public class DatabaseTask extends AbstractTask {

    private static final Logger LOGGER = Logger.getLogger(DatabaseTask.class.getName());

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
        LOGGER.info(() -> "    Executing query: " + query);
        return "Query executed, 5 rows affected";
    }

    @Override
    protected void cleanup(TaskConfig config) {
        // In a real system: close connection, return to pool
        LOGGER.info("    [DatabaseTask] Connection returned to pool");
    }
}
