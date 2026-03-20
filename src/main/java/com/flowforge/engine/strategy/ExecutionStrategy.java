package com.flowforge.engine.strategy;

import com.flowforge.model.TaskConfig;
import com.flowforge.model.TaskResult;
import com.flowforge.task.TaskFactory;

import java.util.List;
import java.util.Map;

/**
 * Strategy interface for workflow execution behavior.
 * the engine selects a strategy at runtime and delegates execution to it.
 * New strategies can be added without modifying the engine (OCP).
 */
public interface ExecutionStrategy {

    /**
     * @return
     */
    String getName();

    /**
     * Executes a list of task configurations using the provided factory.
     *
     * @param taskConfigs the ordered list of task configurations
     * @param factory     the factory to create Task instances
     * @return map of task name → result for each executed task
     */
    Map<String, TaskResult> execute(List<TaskConfig> taskConfigs, TaskFactory factory);
}