package com.flowforge.task.decorator;
import java.util.logging.Logger;
import com.flowforge.model.TaskConfig;
import com.flowforge.model.TaskResult;
import com.flowforge.task.Task;

/**
 * Decorator that adds detailed execution logging around any task.
 *
 * WHY: Not every task needs verbose logging. In production, you might
 * wrap critical tasks with LoggingDecorator and leave simple ones unwrapped.
 *
 * This is task-level logging (entry/exit/timing) — separate from the
 * workflow-level logging done by LoggingListener via EventBus.
 */
public class LoggingDecorator extends TaskDecorator {

    private static final Logger LOGGER = Logger.getLogger(LoggingDecorator.class.getName());

    public LoggingDecorator(Task wrappedTask) {
        super(wrappedTask);
    }

    @Override
    public TaskResult execute(TaskConfig config) {
        LOGGER.info("    [LoggingDecorator] >>> START task: " + getName()
                + " [type=" + getType() + "]");
        long startMs = System.currentTimeMillis();

        TaskResult result = wrappedTask.execute(config);

        long durationMs = System.currentTimeMillis() - startMs;
        String status = result.isSuccess() ? "SUCCESS" : "FAILED";
        LOGGER.info("    [LoggingDecorator] <<< END task: " + getName()
                + " [" + status + ", " + durationMs + "ms]");

        return result;
    }
}