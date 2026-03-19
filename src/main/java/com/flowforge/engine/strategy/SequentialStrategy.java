package com.flowforge.engine.strategy;

import com.flowforge.exception.TaskExecutionException;
import com.flowforge.model.TaskConfig;
import com.flowforge.model.TaskResult;
import com.flowforge.task.Task;
import com.flowforge.task.TaskFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SequentialStrategy implements ExecutionStrategy {

    @Override
    public String getName() {
        return "sequential";
    }

    @Override
    public Map<String, TaskResult> execute(List<TaskConfig> taskConfigs, TaskFactory factory) {
        Map<String, TaskResult> results = new LinkedHashMap<>();

        for (TaskConfig config : taskConfigs) {
            Task task = factory.createTask(config);
            System.out.println("  [sequential] Executing: " + task.getName());

            TaskResult result = task.execute(config);
            results.put(task.getName(), result);

            if (!result.isSuccess()) {
                throw new TaskExecutionException(
                        task.getName(),
                        "Task failed in sequential execution: " + result.getErrorMessage()
                );
            }
        }

        return results;
    }
}