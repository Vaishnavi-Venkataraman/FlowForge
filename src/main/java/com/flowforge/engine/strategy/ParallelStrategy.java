package com.flowforge.engine.strategy;

import com.flowforge.exception.TaskExecutionException;
import com.flowforge.model.TaskConfig;
import com.flowforge.model.TaskResult;
import com.flowforge.task.Task;
import com.flowforge.task.TaskFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Executes all tasks concurrently using a thread pool.
 */
public class ParallelStrategy implements ExecutionStrategy {

    private static final Logger LOGGER = Logger.getLogger(ParallelStrategy.class.getName());
    private static final int THREAD_POOL_SIZE = 4;
    private static final long TIMEOUT_SECONDS = 60;

    @Override
    public String getName() {
        return "parallel";
    }

    @Override
    public Map<String, TaskResult> execute(List<TaskConfig> taskConfigs, TaskFactory factory) {
        Map<String, TaskResult> results = new ConcurrentHashMap<>();
        CountDownLatch latch = new CountDownLatch(taskConfigs.size());

        // FIX: try-with-resources ensures ExecutorService is always closed
        try (ExecutorService executor = Executors.newFixedThreadPool(
                Math.min(THREAD_POOL_SIZE, taskConfigs.size()))) {

            for (TaskConfig config : taskConfigs) {
                executor.submit(() -> {
                    try {
                        Task task = factory.createTask(config);
                        LOGGER.info("[parallel] Executing: " + task.getName()
                                + " on " + Thread.currentThread().getName());
                        TaskResult result = task.execute(config);
                        results.put(task.getName(), result);
                    } catch (Exception e) {
                        results.put(config.getName(),
                                TaskResult.failure("Parallel execution error: " + e.getMessage(),
                                        java.time.Instant.now()));
                    } finally {
                        latch.countDown();
                    }
                });
            }

            try {
                boolean completed = latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                if (!completed) {
                    throw new TaskExecutionException("parallel-batch",
                            "Parallel execution timed out after " + TIMEOUT_SECONDS + "s");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new TaskExecutionException("parallel-batch", "Parallel execution interrupted");
            }
        }

        // Check for any failures
        for (Map.Entry<String, TaskResult> entry : results.entrySet()) {
            if (!entry.getValue().isSuccess()) {
                throw new TaskExecutionException(entry.getKey(),
                        "Failed during parallel execution: " + entry.getValue().getErrorMessage());
            }
        }

        return new LinkedHashMap<>(results);
    }
}