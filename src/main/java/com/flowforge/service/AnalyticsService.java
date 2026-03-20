package com.flowforge.service;
import java.util.logging.Logger;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class AnalyticsService {

    private static final Logger LOGGER = Logger.getLogger(AnalyticsService.class.getName());
    private static final String SERVICE_NAME = "AnalyticsService";

    private final AtomicInteger totalExecutions = new AtomicInteger();
    private final AtomicInteger successCount = new AtomicInteger();
    private final AtomicInteger failureCount = new AtomicInteger();
    private final Map<String, AtomicInteger> perWorkflowCount = new ConcurrentHashMap<>();

    public AnalyticsService(ServiceBus bus) {
        bus.subscribe("analytics", this::handleAnalyticsEvent);
        LOGGER.info(() -> "[" + SERVICE_NAME + "] Started. Listening on: analytics");
    }

    private void handleAnalyticsEvent(ServiceMessage message) {
        String action = message.getAction();
        String workflowName = message.getPayloadValue("workflowName", "unknown");

        totalExecutions.incrementAndGet();
        perWorkflowCount.computeIfAbsent(workflowName, k -> new AtomicInteger()).incrementAndGet();

        switch (action) {
            case "EXECUTION_COMPLETE" -> {
                successCount.incrementAndGet();
                LOGGER.info(() -> "[" + SERVICE_NAME + "] Recorded: SUCCESS for " + workflowName);
            }
            case "EXECUTION_FAILED" -> {
                failureCount.incrementAndGet();
                LOGGER.info(() -> "[" + SERVICE_NAME + "] Recorded: FAILURE for " + workflowName
                        + " — " + message.getPayloadValue("error", ""));
            }
            default -> LOGGER.info(() -> "[" + SERVICE_NAME + "] Recorded: " + action);
        }
    }

    public void printDashboard() {
        LOGGER.info("=== Analytics Dashboard ===");
        LOGGER.info(() -> "  Total executions: " + totalExecutions);
        LOGGER.info(() -> "  Successes: " + successCount);
        LOGGER.info(() -> "  Failures: " + failureCount);
        double rate = totalExecutions.get() > 0
                ? (successCount.get() * 100.0 / totalExecutions.get()) : 0;
        LOGGER.info(String.format("  Success rate: %.1f%%%n", rate));
        LOGGER.info("  Per-workflow breakdown:");
        perWorkflowCount.forEach((name, count) ->
                LOGGER.info(() -> "    " + name + ": " + count + " execution(s)"));
    }
}
