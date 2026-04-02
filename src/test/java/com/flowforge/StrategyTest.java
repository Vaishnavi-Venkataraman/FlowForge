package com.flowforge;

import com.flowforge.engine.strategy.SequentialStrategy;
import com.flowforge.model.TaskConfig;
import com.flowforge.model.TaskResult;
import com.flowforge.task.TaskFactory;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Strategy pattern — SequentialStrategy.
 */
class StrategyTest {

    @Test
    void shouldExecuteTasksSequentially() {
        TaskFactory factory = new TaskFactory();
        SequentialStrategy strategy = new SequentialStrategy();

        List<TaskConfig> tasks = List.of(
                new TaskConfig("step1", "delay", Map.of("seconds", "0")),
                new TaskConfig("step2", "delay", Map.of("seconds", "0"))
        );

        Map<String, TaskResult> results = strategy.execute(tasks, factory);

        assertEquals(2, results.size());
        assertTrue(results.values().stream().allMatch(TaskResult::isSuccess));
    }

    @Test
    void shouldReturnStrategyName() {
        assertEquals("sequential", new SequentialStrategy().getName());
    }
}
