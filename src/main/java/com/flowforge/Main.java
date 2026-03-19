package com.flowforge;

import com.flowforge.engine.WorkflowEngine;
import com.flowforge.engine.strategy.ParallelStrategy;
import com.flowforge.event.EventBus;
import com.flowforge.event.LoggingListener;
import com.flowforge.event.MetricsListener;
import com.flowforge.event.NotificationListener;
import com.flowforge.exception.TaskExecutionException;
import com.flowforge.model.TaskConfig;
import com.flowforge.model.TriggerConfig;
import com.flowforge.model.TriggerType;
import com.flowforge.model.WorkflowDefinition;
import com.flowforge.task.TaskFactory;

import java.util.List;
import java.util.Map;

/**
 * Entry point — demonstrates Observer/Pub-Sub pattern.
 *
 * Key change: engine no longer has ANY System.out.println for logging or
 * notifications. All output comes from EventBus listeners.
 */
public class Main {

    public static void main(String[] args) {
        // --- Wire up the event system ---
        EventBus eventBus = new EventBus();
        LoggingListener logger = new LoggingListener();
        NotificationListener notifier = new NotificationListener();
        MetricsListener metrics = new MetricsListener();

        // Logger listens to everything
        eventBus.subscribeAll(logger);
        // Notifier only cares about workflow start/complete/fail
        eventBus.subscribeAll(notifier);
        // Metrics tracks everything
        eventBus.subscribeAll(metrics);

        // --- Create engine with event bus ---
        TaskFactory factory = new TaskFactory();
        WorkflowEngine engine = new WorkflowEngine(factory, eventBus);
        engine.registerStrategy(new ParallelStrategy());

        // --- Workflow 1: Sequential ETL ---
        WorkflowDefinition etl = new WorkflowDefinition(
                "ETL Pipeline",
                new TriggerConfig(TriggerType.CRON, "0 2 * * *"),
                List.of(
                        new TaskConfig("Extract", "http", Map.of(
                                "url", "https://api.source.com/data", "method", "GET")),
                        new TaskConfig("Transform", "transform", Map.of(
                                "input", "raw_data", "operation", "normalize")),
                        new TaskConfig("Load", "database", Map.of(
                                "query", "INSERT INTO warehouse SELECT * FROM staging"))
                ),
                "sequential"
        );

        System.out.println("\n========== WORKFLOW 1: Sequential ETL ==========\n");
        String id1 = engine.registerWorkflow(etl);
        engine.executeWorkflow(id1);

        // --- Workflow 2: Parallel notifications ---
        WorkflowDefinition notify = new WorkflowDefinition(
                "Multi-Channel Notify",
                new TriggerConfig(TriggerType.EVENT, "order.completed"),
                List.of(
                        new TaskConfig("Email Customer", "email", Map.of(
                                "to", "customer@example.com", "subject", "Order Confirmed")),
                        new TaskConfig("Update CRM", "http", Map.of(
                                "url", "https://crm.api/update", "method", "POST")),
                        new TaskConfig("Log Event", "database", Map.of(
                                "query", "INSERT INTO audit_log(event) VALUES('order_complete')"))
                ),
                "parallel"
        );

        System.out.println("\n========== WORKFLOW 2: Parallel Notify ==========\n");
        String id2 = engine.registerWorkflow(notify);
        engine.executeWorkflow(id2);

        // --- Workflow 3: Will fail ---
        WorkflowDefinition bad = new WorkflowDefinition(
                "Failing Workflow",
                new TriggerConfig(TriggerType.MANUAL, ""),
                List.of(new TaskConfig("Bad Task", "unknown_type", Map.of())),
                "sequential"
        );

        System.out.println("\n========== WORKFLOW 3: Failure Demo ==========\n");
        String id3 = engine.registerWorkflow(bad);
        try {
            engine.executeWorkflow(id3);
        } catch (TaskExecutionException e) {
            System.out.println("(Caught in Main: " + e.getMessage() + ")");
        }

        // --- Metrics report (from listener, not engine) ---
        System.out.println("\n========== Metrics Report ==========\n");
        metrics.printReport();

        // --- Logs (from listener, not engine) ---
        System.out.println("\n========== Event Logs ==========");
        for (String log : logger.getLogs()) {
            System.out.println("  " + log);
        }
    }
}