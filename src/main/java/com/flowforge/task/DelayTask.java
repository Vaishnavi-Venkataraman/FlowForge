package com.flowforge.task;

import com.flowforge.model.TaskConfig;

/**
 * Delay task — introduces a configurable wait into the pipeline.
 * Refactored with Template Method lifecycle.
 */
public class DelayTask extends AbstractTask {

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
        System.out.println("    Waiting " + seconds + " seconds...");
        Thread.sleep(seconds * 1000L);
        return "Delay of " + seconds + "s completed";
    }
}