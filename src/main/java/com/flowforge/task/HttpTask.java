package com.flowforge.task;
import java.util.logging.Logger;
import com.flowforge.model.TaskConfig;

public class HttpTask extends AbstractTask {

    private static final Logger LOGGER = Logger.getLogger(HttpTask.class.getName());

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
        LOGGER.info(() -> "    HTTP " + method + " " + url);
        return "HTTP " + method + " " + url + " → 200 OK";
    }
}
