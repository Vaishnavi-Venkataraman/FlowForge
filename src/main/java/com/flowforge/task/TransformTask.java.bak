package com.flowforge.task;
import java.util.logging.Logger;
import com.flowforge.model.TaskConfig;

public class TransformTask extends AbstractTask {
        private static final Logger LOGGER = Logger.getLogger(TransformTask.class.getName());

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
        LOGGER.info("    Transform: " + operation + " on " + input);
        return "Transformed(" + operation + "): " + input;
    }
}