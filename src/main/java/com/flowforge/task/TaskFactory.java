package com.flowforge.task;

import com.flowforge.exception.TaskExecutionException;
import com.flowforge.model.TaskConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/*
 * PATTERN: Factory Method + Registry
 * - The registry map acts as a configurable factory
 * - New task types can be registered at runtime
 * - The engine only depends on the factory, not concrete tasks
 */
public class TaskFactory {

    private final Map<String, Function<String, Task>> registry = new HashMap<>();

    public TaskFactory() {
        // Register built-in task types
        registerTaskType("http", HttpTask::new);
        registerTaskType("email", EmailTask::new);
        registerTaskType("transform", TransformTask::new);
        registerTaskType("database", DatabaseTask::new);
        registerTaskType("delay", DelayTask::new);
    }

    /**
     * Registers a task type with its constructor function.
     * This is the extension point — new task types are added here,
     * not by modifying the engine or a switch statement.
     *
     * @param type    the task type identifier
     * @param creator function that takes a task name and returns a Task
     */
    public void registerTaskType(String type, Function<String, Task> creator) {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Task type cannot be null or blank");
        }
        if (creator == null) {
            throw new IllegalArgumentException("Task creator cannot be null");
        }
        registry.put(type, creator);
    }

    /**
     * Creates a Task from configuration.
     *
     * @param config the task configuration
     * @return a new Task instance
     * @throws TaskExecutionException if the task type is unknown
     */
    public Task createTask(TaskConfig config) {
        Function<String, Task> creator = registry.get(config.getType());
        if (creator == null) {
            throw new TaskExecutionException(
                    config.getName(),
                    "Unknown task type: " + config.getType()
                            + ". Registered types: " + registry.keySet()
            );
        }
        return creator.apply(config.getName());
    }

    /**
     * Checks whether a task type is registered.
     */
    public boolean isTypeRegistered(String type) {
        return registry.containsKey(type);
    }
}