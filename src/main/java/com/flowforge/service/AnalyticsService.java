package com.flowforge.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class AnalyticsService {

    private static final String SERVICE_NAME = "AnalyticsService";

    private final AtomicInteger totalExecutions = new AtomicInteger();
    private final AtomicInteger successCount = new AtomicInteger();
    private final AtomicInteger failureCount = new AtomicInteger();
    private final Map<String, AtomicInteger> perWorkflowCount = new ConcurrentHashMap<>();

    public AnalyticsService(ServiceBus bus) {
        bus.subscribe("analytics", this::handleAnalyticsEvent);
        System.out.println("[" + SERVICE_NAME + "] Started. Listening on: analytics");
    }

    private void handleAnalyticsEvent(ServiceMessage message) {
        String action = message.getAction();
        String workflowName = message.getPayloadValue("workflowName", "unknown");

        totalExecutions.incrementAndGet();
        perWorkflowCount.computeIfAbsent(workflowName, k -> new AtomicInteger()).incrementAndGet();

        switch (action) {
            case "EXECUTION_COMPLETE" -> {
                successCount.incrementAndGet();
                System.out.println("[" + SERVICE_NAME + "] Recorded: SUCCESS for " + workflowName);
            }
            case "EXECUTION_FAILED" -> {
                failureCount.incrementAndGet();
                System.out.println("[" + SERVICE_NAME + "] Recorded: FAILURE for " + workflowName
                        + " — " + message.getPayloadValue("error", ""));
            }
            default -> System.out.println("[" + SERVICE_NAME + "] Recorded: " + action);
        }
    }

    public void printDashboard() {
        System.out.println("=== Analytics Dashboard ===");
        System.out.println("  Total executions: " + totalExecutions);
        System.out.println("  Successes: " + successCount);
        System.out.println("  Failures: " + failureCount);
        double rate = totalExecutions.get() > 0
                ? (successCount.get() * 100.0 / totalExecutions.get()) : 0;
        System.out.printf("  Success rate: %.1f%%\n", rate);
        System.out.println("  Per-workflow breakdown:");
        perWorkflowCount.forEach((name, count) ->
                System.out.println("    " + name + ": " + count + " execution(s)"));
    }
}