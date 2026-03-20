package com.flowforge.task;
import java.util.logging.Logger;
import com.flowforge.model.TaskConfig;
import com.flowforge.model.TaskResult;

import java.time.Instant;

public abstract class AbstractTask implements Task {

    private final String name;
    private static final Logger LOGGER = Logger.getLogger(AbstractTask.class.getName());

    protected AbstractTask(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Task name cannot be null or blank");
        }
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * Template method — defines the fixed lifecycle order.
     */
    @Override
    public final TaskResult execute(TaskConfig config) {
        Instant start = Instant.now();

        try {
            // Step 1: Validate input
            validate(config);

            // Step 2: Perform the actual work (abstract — must be implemented)
            String output = doExecute(config);

            // Step 3: Cleanup resources
            cleanup(config);

            return TaskResult.success(output, start);

        } catch (IllegalArgumentException e) {
            // Validation failure
            return TaskResult.failure("Validation failed: " + e.getMessage(), start);
        } catch (Exception e) {
            // Execution failure — still run cleanup
            try {
                cleanup(config);
            } catch (Exception cleanupEx) {
                // Log but don't mask the original exception
                LOGGER.warning(() -> "Cleanup failed for task " + name + ": " + cleanupEx.getMessage());
            }
            return TaskResult.failure("Execution failed: " + e.getMessage(), start);
        }
    }

    // ========== HOOK METHODS ==========

    /**
     * Validates task configuration before execution.
     * Default: no-op. Override to add required parameter checks.
     *
     * @param config the task configuration
     * @throws IllegalArgumentException if validation fails
     */
    protected void validate(TaskConfig config) {
        // Default: no validation. Subclasses override for specific checks.
    }

    /**
     * Performs the actual task work.
     * This is the only REQUIRED override for subclasses.
     *
     * @param config the task configuration
     * @return output string describing the result
     * @throws Exception if execution fails
     */
    protected abstract String doExecute(TaskConfig config) throws Exception; // NOSONAR - broad catch needed for plugin extensibility

    /**
     * Cleans up resources after execution (success or failure).
     * Default: no-op. Override for resource cleanup (close connections, etc.).
     *
     * @param config the task configuration
     */
    protected void cleanup(TaskConfig config) {
        // Default: no cleanup needed. Subclasses override as needed.
    }
}
