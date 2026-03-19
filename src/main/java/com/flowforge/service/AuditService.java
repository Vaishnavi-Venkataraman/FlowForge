package com.flowforge.service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AuditService {

    private static final String SERVICE_NAME = "AuditService";
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
                    .withZone(ZoneId.systemDefault());

    private final List<String> auditLog = Collections.synchronizedList(new ArrayList<>());

    public AuditService(ServiceBus bus) {
        bus.subscribe("audit", this::handleAuditEvent);
        System.out.println("[" + SERVICE_NAME + "] Started. Listening on: audit");
    }

    private void handleAuditEvent(ServiceMessage message) {
        String entry = FORMATTER.format(message.getTimestamp())
                + " | " + message.getAction()
                + " | from=" + message.getSourceService()
                + " | " + message.getPayload();
        auditLog.add(entry);
        System.out.println("[" + SERVICE_NAME + "] " + entry);
    }

    public void printAuditLog() {
        System.out.println("=== Audit Log (" + auditLog.size() + " entries) ===");
        for (String entry : auditLog) {
            System.out.println("  " + entry);
        }
    }

    public int getEntryCount() {
        return auditLog.size();
    }
}