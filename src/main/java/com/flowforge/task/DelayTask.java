package com.flowforge.task;
import java.util.logging.Logger;
import com.flowforge.model.TaskConfig;

/**
 * Delay task — introduces a configurable wait into the pipeline.
 * Refactored with Template Method lifecycle.
 */
public class DelayTask extends AbstractTask {

    private static final Logger LOGGER = Logger.getLogger(DelayTask.class.getName());
    public DelayTask(String name) {
        super(name);
    }

    @Override
    public String getType() {
        return "delay";
    }

    @Override
    protected void validate(TaskConfig config) {
        String seconds = config.getParameter("seconds", "1");
        try {
            int val = Integer.parseInt(seconds);
            if (val < 0) {
                throw new IllegalArgumentException("Delay seconds must be non-negative, got: " + val);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Delay seconds must be a number, got: " + seconds);
        }
    }

    @Override
    protected String doExecute(TaskConfig config) throws InterruptedException {
        int seconds = Integer.parseInt(config.getParameter("seconds", "1"));
        LOGGER.info("    Waiting " + seconds + " seconds...");
        Thread.sleep(seconds * 1000L);
        return "Delay of " + seconds + "s completed";
    }
}