package com.flowforge.task;

import com.flowforge.model.TaskConfig;
import com.flowforge.model.TaskResult;

import java.time.Instant;

/**
 * Simulates sending an email notification.
 */
public class EmailTask implements Task {

    private final String name;

    public EmailTask(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getType() {
        return "email";
    }

    @Override
    public TaskResult execute(TaskConfig config) {
        Instant start = Instant.now();
        String to = config.getRequiredParameter("to");
        String subject = config.getRequiredParameter("subject");
        System.out.println("  Sending email to " + to + ": " + subject);
        return TaskResult.success("Email sent to " + to, start);
    }
}