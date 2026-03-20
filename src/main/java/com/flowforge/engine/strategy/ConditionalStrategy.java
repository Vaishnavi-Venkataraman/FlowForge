package com.flowforge.engine.strategy;
import java.util.logging.Logger;
import com.flowforge.exception.TaskExecutionException;
import com.flowforge.model.TaskConfig;
import com.flowforge.model.TaskResult;
import com.flowforge.task.Task;
import com.flowforge.task.TaskFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class ConditionalStrategy implements ExecutionStrategy {
    private static final Logger LOGGER = Logger.getLogger(ConditionalStrategy.class.getName()); 
    private final Predicate<TaskConfig> condition;
    private final String conditionDescription;

    /**
     * @param condition            predicate to evaluate per task
     * @param conditionDescription human-readable description for logging
     */
    public ConditionalStrategy(Predicate<TaskConfig> condition, String conditionDescription) {
        if (condition == null) {
            throw new IllegalArgumentException("Condition predicate cannot be null");
        }
        this.condition = condition;
        this.conditionDescription = conditionDescription != null
                ? conditionDescription : "custom condition";
    }

    @Override
    public String getName() {
        return "conditional(" + conditionDescription + ")";
    }

    @Override
    public Map<String, TaskResult> execute(List<TaskConfig> taskConfigs, TaskFactory factory) {
        Map<String, TaskResult> results = new LinkedHashMap<>();

        for (TaskConfig config : taskConfigs) {
            if (!condition.test(config)) {
                LOGGER.info("  [conditional] SKIPPED: " + config.getName()
                        + " (condition not met: " + conditionDescription + ")");
                results.put(config.getName(),
                        TaskResult.success("Skipped - condition not met", java.time.Instant.now()));
                continue;
            }

            Task task = factory.createTask(config);
            LOGGER.info("  [conditional] Executing: " + task.getName());

            TaskResult result = task.execute(config);
            results.put(task.getName(), result);

            if (!result.isSuccess()) {
                throw new TaskExecutionException(
                        task.getName(),
                        "Task failed in conditional execution: " + result.getErrorMessage()
                );
            }
        }

        return results;
    }
}