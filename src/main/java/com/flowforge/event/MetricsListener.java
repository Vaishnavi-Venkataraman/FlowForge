package com.flowforge.event;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/* Collects execution metrics from workflow events. */
public class MetricsListener implements EventListener {

    private final AtomicInteger workflowsStarted = new AtomicInteger();
    private final AtomicInteger workflowsCompleted = new AtomicInteger();
    private final AtomicInteger workflowsFailed = new AtomicInteger();
    private final AtomicInteger tasksStarted = new AtomicInteger();
    private final AtomicInteger tasksCompleted = new AtomicInteger();
    private final AtomicInteger tasksFailed = new AtomicInteger();
    private final AtomicInteger tasksSkipped = new AtomicInteger();
    private final AtomicLong totalTaskDurationMs = new AtomicLong();

    @Override
    public void onEvent(WorkflowEvent event) {
        switch (event.getType()) {
            case WORKFLOW_STARTED -> workflowsStarted.incrementAndGet();
            case WORKFLOW_COMPLETED -> workflowsCompleted.incrementAndGet();
            case WORKFLOW_FAILED -> workflowsFailed.incrementAndGet();
            case TASK_STARTED -> tasksStarted.incrementAndGet();
            case TASK_COMPLETED -> {
                tasksCompleted.incrementAndGet();
                String duration = event.getMetadata().get("durationMs");
                if (duration != null) {
                    totalTaskDurationMs.addAndGet(Long.parseLong(duration));
                }
            }
            case TASK_FAILED -> tasksFailed.incrementAndGet();
            case TASK_SKIPPED -> tasksSkipped.incrementAndGet();
            default -> { /* ignore */ }
        }
    }

    public void printReport() {
        System.out.println("=== Execution Metrics ===");
        System.out.println("Workflows — Started: " + workflowsStarted
                + ", Completed: " + workflowsCompleted
                + ", Failed: " + workflowsFailed);
        System.out.println("Tasks     — Started: " + tasksStarted
                + ", Completed: " + tasksCompleted
                + ", Failed: " + tasksFailed
                + ", Skipped: " + tasksSkipped);
        System.out.println("Total task execution time: " + totalTaskDurationMs + "ms");
    }
}