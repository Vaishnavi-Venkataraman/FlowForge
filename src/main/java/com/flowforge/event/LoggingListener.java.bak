package com.flowforge.event;
import java.util.logging.Logger;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LoggingListener implements EventListener {
    private static final Logger LOGGER = Logger.getLogger(LoggingListener.class.getName());
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

    private final List<String> logs = new ArrayList<>();

    @Override
    public void onEvent(WorkflowEvent event) {
        String entry = FORMATTER.format(event.getTimestamp())
                + " [" + event.getType() + "] "
                + event.getMessage();
        logs.add(entry);
        LOGGER.info("LOG: " + entry);
    }

    public List<String> getLogs() {
        return Collections.unmodifiableList(logs);
    }
}