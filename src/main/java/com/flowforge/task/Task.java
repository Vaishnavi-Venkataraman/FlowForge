package com.flowforge.task;

import com.flowforge.model.TaskConfig;
import com.flowforge.model.TaskResult;

/**
 * Core abstraction for an executable task.
 *
 * WHY: This is the key interface that breaks the if-else chain in
 * WorkflowEngine.executeWorkflow(). Instead of the engine knowing
 * how to execute every task type, each task type implements this
 * interface. The engine just calls execute() — it doesn't care
 * whether it's an HTTP call, email, or database query.
 *
 * This follows the Open/Closed Principle: the engine is closed for
 * modification but open for extension via new Task implementations.
 */
public interface Task {

    /**
     * @return the unique name of this task instance
     */
    String getName();

    /**
     * @return the task type identifier (e.g., "http", "email")
     */
    String getType();

    /**
     * Executes the task with the given configuration.
     *
     * @param config the task configuration
     * @return result of execution
     */
    TaskResult execute(TaskConfig config);
}