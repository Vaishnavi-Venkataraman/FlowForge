package com.flowforge.task;
import java.util.logging.Logger;
import com.flowforge.model.TaskConfig;

/**
 * Email task — refactored with Template Method lifecycle.
 * Validates required 'to' and 'subject' parameters before execution.
 */
public class EmailTask extends AbstractTask {

    private static final Logger LOGGER = Logger.getLogger(EmailTask.class.getName());
    public EmailTask(String name) {
        super(name);
    }

    @Override
    public String getType() {
        return "email";
    }

    @Override
    protected void validate(TaskConfig config) {
        config.getRequiredParameter("to");
        config.getRequiredParameter("subject");
    }

    @Override
    protected String doExecute(TaskConfig config) {
        String to = config.getRequiredParameter("to");
        String subject = config.getRequiredParameter("subject");
        LOGGER.info(() -> "    Sending email to " + to + ": " + subject);
        return "Email sent to " + to;
    }
}
