package com.flowforge.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NotificationService {

    private static final String SERVICE_NAME = "NotificationService";
    private static final String KEY_WORKFLOW_NAME = "workflowName";
    private final List<String> sentNotifications = new ArrayList<>();

    public NotificationService(ServiceBus bus) {
        bus.subscribe("notifications", this::handleNotification);
        System.out.println("[" + SERVICE_NAME + "] Started. Listening on: notifications");
    }

    private void handleNotification(ServiceMessage message) {
        String action = message.getAction();
        Map<String, String> payload = message.getPayload();

        String notification = switch (action) {
            case "WORKFLOW_COMPLETED" ->
                    "✓ SUCCESS: Workflow '" + payload.getOrDefault(KEY_WORKFLOW_NAME, "?") + "' completed";

            case "WORKFLOW_FAILED" ->
                    "✗ FAILURE: Workflow '" + payload.getOrDefault(KEY_WORKFLOW_NAME, "?")
                            + "' — " + payload.getOrDefault("error", "unknown error");

            case "TRIGGER_FIRED" ->
                    "⚡ TRIGGER: " + payload.getOrDefault("triggerType", "?")
                            + " fired for workflow '" + payload.getOrDefault(KEY_WORKFLOW_NAME, "?") + "'";

            default -> "ℹ " + action + ": " + payload;
        };

        sentNotifications.add(notification);
        System.out.println("[" + SERVICE_NAME + "] " + notification);
    }

    public List<String> getSentNotifications() {
        return List.copyOf(sentNotifications);
    }

    public void printSummary() {
        System.out.println("=== Notification Summary (" + sentNotifications.size() + " sent) ===");
        for (String n : sentNotifications) {
            System.out.println("  " + n);
        }
    }
}