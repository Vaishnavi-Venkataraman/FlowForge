package com.flowforge.service;
import java.util.logging.Logger;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AuditService {
    private static final Logger LOGGER = Logger.getLogger(AuditService.class.getName());
    private static final String SERVICE_NAME = "AuditService";
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
                    .withZone(ZoneId.systemDefault());

    private final List<String> auditLog = Collections.synchronizedList(new ArrayList<>());

    public AuditService(ServiceBus bus) {
        bus.subscribe("audit", this::handleAuditEvent);
        LOGGER.info("[" + SERVICE_NAME + "] Started. Listening on: audit");
    }

    private void handleAuditEvent(ServiceMessage message) {
        String entry = FORMATTER.format(message.getTimestamp())
                + " | " + message.getAction()
                + " | from=" + message.getSourceService()
                + " | " + message.getPayload();
        auditLog.add(entry);
        LOGGER.info("[" + SERVICE_NAME + "] " + entry);
    }

    public void printAuditLog() {
        LOGGER.info("=== Audit Log (" + auditLog.size() + " entries) ===");
        for (String entry : auditLog) {
            LOGGER.info("  " + entry);
        }
    }

    public int getEntryCount() {
        return auditLog.size();
    }
}