package com.flowforge.task;

import com.flowforge.model.TaskConfig;

/**
 * HTTP task — refactored to use Template Method lifecycle.
 *
 * BEFORE (Commit 2-6): implemented execute() directly, no validation.
 * NOW: extends AbstractTask, overrides validate() and doExecute().
 * The lifecycle (validate → execute → cleanup) is guaranteed by AbstractTask.
 */
public class HttpTask extends AbstractTask {

    public HttpTask(String name) {
        super(name);
    }

    @Override
    public String getType() {
        return "http";
    }

    @Override
    protected void validate(TaskConfig config) {
        config.getRequiredParameter("url");
        // method is optional, defaults to GET
    }

    @Override
    protected String doExecute(TaskConfig config) {
        String url = config.getRequiredParameter("url");
        String method = config.getParameter("method", "GET");
        System.out.println("    HTTP " + method + " " + url);
        return "HTTP " + method + " " + url + " → 200 OK";
    }
}