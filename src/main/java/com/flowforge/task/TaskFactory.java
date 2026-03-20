package com.flowforge.task;

import com.flowforge.exception.TaskExecutionException;
import com.flowforge.model.TaskConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public class TaskFactory {

    private final Map<String, Function<String, Task>> registry = new HashMap<>();
    private UnaryOperator<Task> globalDecorator = UnaryOperator.identity(); // no-op default

    public TaskFactory() {
        registerTaskType("http", HttpTask::new);
        registerTaskType("email", EmailTask::new);
        registerTaskType("transform", TransformTask::new);
        registerTaskType("database", DatabaseTask::new);
        registerTaskType("delay", DelayTask::new);
    }

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
     * Sets a global decorator that wraps EVERY task created by this factory.
     * @param decorator function that wraps a Task with decorators
     */
    public void setGlobalDecorator(UnaryOperator<Task> decorator) {
        this.globalDecorator = decorator != null ? decorator : UnaryOperator.identity();
    }

    public Task createTask(TaskConfig config) {
        Function<String, Task> creator = registry.get(config.getType());
        if (creator == null) {
            throw new TaskExecutionException(
                    config.getName(),
                    "Unknown task type: " + config.getType()
                            + ". Registered types: " + registry.keySet()
            );
        }

        Task task = creator.apply(config.getName());

        // Apply global decorator chain
        return globalDecorator.apply(task);
    }

    public boolean isTypeRegistered(String type) {
        return registry.containsKey(type);
    }
}