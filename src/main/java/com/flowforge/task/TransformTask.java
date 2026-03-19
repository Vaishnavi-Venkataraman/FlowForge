package com.flowforge.task;

import com.flowforge.model.TaskConfig;

/**
 * Data transformation task — refactored with Template Method lifecycle.
 */
public class TransformTask extends AbstractTask {

    public TransformTask(String name) {
        super(name);
    }

    @Override
    public String getType() {
        return "transform";
    }

    @Override
    protected void validate(TaskConfig config) {
        config.getRequiredParameter("input");
    }

    @Override
    protected String doExecute(TaskConfig config) {
        String input = config.getRequiredParameter("input");
        String operation = config.getParameter("operation", "identity");
        System.out.println("    Transform: " + operation + " on " + input);
        return "Transformed(" + operation + "): " + input;
    }
}