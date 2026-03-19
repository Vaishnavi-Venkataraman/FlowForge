package com.flowforge.event;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Listens to all events and maintains a structured log.
 * WHY: Replaces the inline log() method and System.out.println calls
 */
public class LoggingListener implements EventListener {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

    private final List<String> logs = new ArrayList<>();

    @Override
    public void onEvent(WorkflowEvent event) {
        String entry = FORMATTER.format(event.getTimestamp())
                + " [" + event.getType() + "] "
                + event.getMessage();
        logs.add(entry);
        System.out.println("LOG: " + entry);
    }

    public List<String> getLogs() {
        return Collections.unmodifiableList(logs);
    }
}