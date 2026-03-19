package com.flowforge;

import com.flowforge.engine.WorkflowBuilder;
import com.flowforge.engine.WorkflowEngine;
import com.flowforge.engine.strategy.ParallelStrategy;
import com.flowforge.event.EventBus;
import com.flowforge.event.LoggingListener;
import com.flowforge.event.MetricsListener;
import com.flowforge.event.NotificationListener;
import com.flowforge.exception.TaskExecutionException;
import com.flowforge.model.WorkflowDefinition;
import com.flowforge.task.TaskFactory;

/**
 * Entry point — demonstrates Builder pattern for readable workflow construction.
 *
 * COMPARE (Commit 5 — verbose):
 *   new WorkflowDefinition(
 *       "ETL Pipeline",
 *       new TriggerConfig(TriggerType.CRON, "0 2 * * *"),
 *       List.of(
 *           new TaskConfig("Extract", "http", Map.of("url","...","method","GET")),
 *           new TaskConfig("Transform", "transform", Map.of("input","...","operation","..."))
 *       ),
 *       "sequential"
 *   );
 *
 * WITH BUILDER (Commit 6 — readable):
 *   WorkflowBuilder.create()
 *       .name("ETL Pipeline")
 *       .cronTrigger("0 2 * * *")
 *       .addHttpGet("Extract", "https://...")
 *       .addTransformTask("Transform", "raw", "normalize")
 *       .sequential()
 *       .build();
 */
public class Main {

    public static void main(String[] args) {
        // --- Wire event system ---
        EventBus eventBus = new EventBus();
        LoggingListener logger = new LoggingListener();
        MetricsListener metrics = new MetricsListener();
        eventBus.subscribeAll(logger);
        eventBus.subscribeAll(new NotificationListener());
        eventBus.subscribeAll(metrics);

        // --- Create engine ---
        TaskFactory factory = new TaskFactory();
        WorkflowEngine engine = new WorkflowEngine(factory, eventBus);
        engine.registerStrategy(new ParallelStrategy());

        // ============================================================
        // WORKFLOW 1: ETL Pipeline (Sequential) — built with builder
        // ============================================================
        WorkflowDefinition etl = WorkflowBuilder.create()
                .name("ETL Pipeline")
                .cronTrigger("0 2 * * *")
                .addHttpGet("Extract from API", "https://api.source.com/data")
                .addTransformTask("Normalize Data", "raw_data", "normalize")
                .addTransformTask("Validate Schema", "normalized", "validate")
                .addDatabaseTask("Load to Warehouse", "INSERT INTO warehouse SELECT * FROM staging")
                .sequential()
                .build();

        System.out.println("\n========== WORKFLOW 1: Sequential ETL ==========\n");
        engine.registerWorkflow(etl);
        engine.executeWorkflow(etl.getId());

        // ============================================================
        // WORKFLOW 2: Multi-channel notification (Parallel)
        // ============================================================
        WorkflowDefinition notify = WorkflowBuilder.create()
                .name("Order Confirmation")
                .eventTrigger("order.completed")
                .addEmailTask("Email Customer", "customer@shop.com", "Your order is confirmed!")
                .addHttpPost("Update CRM", "https://crm.api/customers/update")
                .addDatabaseTask("Audit Log", "INSERT INTO audit(event) VALUES('order_confirm')")
                .addHttpPost("Notify Warehouse", "https://warehouse.api/fulfill")
                .parallel()
                .build();

        System.out.println("\n========== WORKFLOW 2: Parallel Notifications ==========\n");
        engine.registerWorkflow(notify);
        engine.executeWorkflow(notify.getId());

        // ============================================================
        // WORKFLOW 3: Data Processing with delay
        // ============================================================
        WorkflowDefinition dataProc = WorkflowBuilder.create()
                .name("Hourly Data Sync")
                .cronTrigger("0 * * * *")
                .addHttpGet("Fetch Updates", "https://api.partner.com/changes")
                .addTransformTask("Merge Changes", "updates", "merge")
                .addDatabaseTask("Apply to DB", "CALL sp_apply_changes()")
                .addEmailTask("Report Done", "ops@company.com", "Hourly sync complete")
                .sequential()
                .build();

        System.out.println("\n========== WORKFLOW 3: Hourly Data Sync ==========\n");
        engine.registerWorkflow(dataProc);
        engine.executeWorkflow(dataProc.getId());

        // ============================================================
        // WORKFLOW 4: Builder validation — missing fields
        // ============================================================
        System.out.println("\n========== WORKFLOW 4: Builder Validation Demo ==========\n");
        try {
            WorkflowBuilder.create()
                    .name("Invalid Workflow")
                    // No trigger defined
                    // No tasks defined
                    .build();
        } catch (IllegalStateException e) {
            System.out.println("Builder validation caught:\n" + e.getMessage());
        }

        // ============================================================
        // METRICS
        // ============================================================
        System.out.println("\n========== Metrics Report ==========\n");
        metrics.printReport();
    }
}