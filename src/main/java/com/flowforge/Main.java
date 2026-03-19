package com.flowforge;

import com.flowforge.engine.WorkflowBuilder;
import com.flowforge.engine.WorkflowEngine;
import com.flowforge.engine.strategy.ParallelStrategy;
import com.flowforge.event.EventBus;
import com.flowforge.event.LoggingListener;
import com.flowforge.event.MetricsListener;
import com.flowforge.event.NotificationListener;
import com.flowforge.model.WorkflowDefinition;
import com.flowforge.pipeline.*;
import com.flowforge.task.TaskFactory;
import com.flowforge.task.decorator.LoggingDecorator;
import com.flowforge.task.decorator.RetryDecorator;

import java.util.Map;
import java.util.Set;

public class Main {

    public static void main(String[] args) {
        // --- Wire event system ---
        EventBus eventBus = new EventBus();
        MetricsListener metrics = new MetricsListener();
        eventBus.subscribeAll(new LoggingListener());
        eventBus.subscribeAll(new NotificationListener());
        eventBus.subscribeAll(metrics);

        // --- Create factory with decorators ---
        TaskFactory factory = new TaskFactory();
        factory.setGlobalDecorator(task ->
                new LoggingDecorator(new RetryDecorator(task, 1, 100))
        );

        // --- Create engine ---
        WorkflowEngine engine = new WorkflowEngine(factory, eventBus);
        engine.registerStrategy(new ParallelStrategy());

        // --- Build the pre-execution pipeline ---
        Pipeline pipeline = new Pipeline()
                .addHandler(new ValidationHandler(factory))
                .addHandler(new AuthorizationHandler("admin", Set.of("Admin-Only Workflow")))
                .addHandler(new RateLimitHandler(5000))   // 5 second cooldown
                .addHandler(new TransformHandler());

        engine.setPipeline(pipeline);
        System.out.println("Pipeline handlers: " + pipeline.getHandlerNames());

        // SCENARIO 1: Normal execution — all pipeline handlers pass
        // Note: query uses ${timestamp} placeholder → enriched by TransformHandler
        WorkflowDefinition etl = WorkflowBuilder.create()
                .name("ETL Pipeline")
                .cronTrigger("0 2 * * *")
                .addHttpGet("Extract Data", "https://api.source.com/data")
                .addTransformTask("Normalize", "raw_data", "normalize")
                .addDatabaseTask("Load", "INSERT INTO warehouse VALUES('${timestamp}', '${workflow.name}')")
                .sequential()
                .build();

        System.out.println("\n========== SCENARIO 1: Normal Pipeline Execution ==========\n");
        engine.registerWorkflow(etl);
        engine.executeWorkflow(etl.getId());

        // SCENARIO 2: Authorization failure — user "intern" tries restricted workflow
        Pipeline restrictedPipeline = new Pipeline()
                .addHandler(new ValidationHandler(factory))
                .addHandler(new AuthorizationHandler("intern", Set.of("Admin-Only Workflow")))
                .addHandler(new TransformHandler());

        // Create a separate engine with restricted pipeline for this demo
        WorkflowEngine restrictedEngine = new WorkflowEngine(factory, eventBus);
        restrictedEngine.setPipeline(restrictedPipeline);

        WorkflowDefinition adminWorkflow = WorkflowBuilder.create()
                .name("Admin-Only Workflow")
                .manualTrigger()
                .addDatabaseTask("Drop Tables", "DROP TABLE users")
                .sequential()
                .build();

        System.out.println("\n========== SCENARIO 2: Authorization Denied ==========\n");
        restrictedEngine.registerWorkflow(adminWorkflow);
        restrictedEngine.executeWorkflow(adminWorkflow.getId());

        // SCENARIO 3: Rate limiting — same workflow executed twice rapidly
        WorkflowDefinition quickJob = WorkflowBuilder.create()
                .name("Quick Job")
                .webhookTrigger("/api/webhook/quick")
                .addHttpGet("Ping", "https://api.health.com/ping")
                .sequential()
                .build();

        System.out.println("\n========== SCENARIO 3: Rate Limiting ==========\n");
        engine.registerWorkflow(quickJob);
        System.out.println("--- First execution (should pass) ---");
        engine.executeWorkflow(quickJob.getId());

        // Reset status so it's executable again
        quickJob.setStatus(com.flowforge.model.WorkflowStatus.CREATED);
        System.out.println("\n--- Second execution immediately (should be rate limited) ---");
        engine.executeWorkflow(quickJob.getId());

        // METRICS
        System.out.println("\n========== Metrics Report ==========\n");
        metrics.printReport();
    }
}