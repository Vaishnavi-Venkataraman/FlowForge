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
import com.flowforge.task.decorator.LoggingDecorator;
import com.flowforge.task.decorator.RetryDecorator;

/**
 * Entry point — demonstrates:
 *
 * 1. TEMPLATE METHOD: All tasks now follow validate() → doExecute() → cleanup()
 *    lifecycle via AbstractTask. DatabaseTask shows cleanup() hook in action.
 *
 * 2. DECORATOR: Tasks wrapped with LoggingDecorator and RetryDecorator
 *    without modifying any task class.
 *    Composition: LoggingDecorator(RetryDecorator(actualTask))
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

        // --- Create factory with global decorators ---
        TaskFactory factory = new TaskFactory();

        // DECORATOR DEMO: wrap ALL tasks with logging + retry
        // Composition order: LoggingDecorator → RetryDecorator → actual task
        // So logging captures the full retry sequence
        factory.setGlobalDecorator(task ->
                new LoggingDecorator(new RetryDecorator(task, 2, 100))
        );

        // --- Create engine ---
        WorkflowEngine engine = new WorkflowEngine(factory, eventBus);
        engine.registerStrategy(new ParallelStrategy());

        // ============================================================
        // WORKFLOW 1: ETL with Template Method lifecycle
        // Notice: DatabaseTask shows cleanup() hook (connection returned to pool)
        // ============================================================
        WorkflowDefinition etl = WorkflowBuilder.create()
                .name("ETL Pipeline (Template Method Demo)")
                .cronTrigger("0 2 * * *")
                .addHttpGet("Extract from API", "https://api.source.com/data")
                .addTransformTask("Normalize Data", "raw_data", "normalize")
                .addDatabaseTask("Load to Warehouse", "INSERT INTO warehouse SELECT * FROM staging")
                .sequential()
                .build();

        System.out.println("\n========== WORKFLOW 1: Template Method Lifecycle ==========\n");
        engine.registerWorkflow(etl);
        engine.executeWorkflow(etl.getId());

        // ============================================================
        // WORKFLOW 2: Parallel with decorators
        // All tasks get LoggingDecorator + RetryDecorator automatically
        // ============================================================
        WorkflowDefinition notify = WorkflowBuilder.create()
                .name("Parallel Notifications (Decorator Demo)")
                .eventTrigger("order.completed")
                .addEmailTask("Email Customer", "customer@shop.com", "Order Confirmed!")
                .addHttpPost("Update CRM", "https://crm.api/update")
                .addDatabaseTask("Audit Log", "INSERT INTO audit(event) VALUES('order_confirm')")
                .parallel()
                .build();

        System.out.println("\n========== WORKFLOW 2: Decorated Parallel Execution ==========\n");
        engine.registerWorkflow(notify);
        engine.executeWorkflow(notify.getId());

        // ============================================================
        // WORKFLOW 3: Validation failure demo (Template Method validate())
        // The http task requires 'url' parameter — missing here triggers validation
        // ============================================================
        WorkflowDefinition badValidation = WorkflowBuilder.create()
                .name("Validation Failure Demo")
                .manualTrigger()
                .addTask("Missing URL Task", "http", java.util.Map.of(
                        "method", "GET"
                        // "url" is missing! validate() will catch this
                ))
                .sequential()
                .build();

        System.out.println("\n========== WORKFLOW 3: Validation Failure ==========\n");
        engine.registerWorkflow(badValidation);
        try {
            engine.executeWorkflow(badValidation.getId());
        } catch (TaskExecutionException e) {
            System.out.println("(Caught: " + e.getMessage() + ")");
        }

        // ============================================================
        // METRICS
        // ============================================================
        System.out.println("\n========== Metrics Report ==========\n");
        metrics.printReport();
    }
}