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
import com.flowforge.pipeline.*;
import com.flowforge.plugin.PluginContext;
import com.flowforge.plugin.PluginRegistry;
import com.flowforge.plugin.builtin.AuditTrailPlugin;
import com.flowforge.plugin.builtin.FileOperationsPlugin;
import com.flowforge.plugin.builtin.SlackNotificationPlugin;
import com.flowforge.task.TaskFactory;
import com.flowforge.task.decorator.LoggingDecorator;

import java.util.Map;
import java.util.Set;

public class Main {

    public static void main(String[] args) {

        // ============================================================
        // STEP 1: Create core components (the "micro" kernel)
        // ============================================================
        EventBus eventBus = new EventBus();
        MetricsListener metrics = new MetricsListener();
        eventBus.subscribeAll(new LoggingListener());
        eventBus.subscribeAll(new NotificationListener());
        eventBus.subscribeAll(metrics);

        TaskFactory factory = new TaskFactory();
        factory.setGlobalDecorator(LoggingDecorator::new);

        Pipeline pipeline = new Pipeline()
                .addHandler(new ValidationHandler(factory))
                .addHandler(new AuthorizationHandler("admin", Set.of()))
                .addHandler(new TransformHandler());

        WorkflowEngine engine = new WorkflowEngine(factory, eventBus);
        engine.setPipeline(pipeline);
        engine.registerStrategy(new ParallelStrategy());

        // ============================================================
        // STEP 2: Create the plugin context (extension hooks)
        // ============================================================
        PluginContext pluginContext = new PluginContext(
                factory,
                eventBus,
                pipeline,
                engine::registerStrategy  // method reference as StrategyRegistrar
        );

        // ============================================================
        // STEP 3: Register plugins
        // ============================================================
        System.out.println("========== Plugin Registration ==========\n");
        PluginRegistry registry = new PluginRegistry(pluginContext);

        AuditTrailPlugin auditPlugin = new AuditTrailPlugin();

        registry.register(new SlackNotificationPlugin(
                "https://hooks.slack.com/services/XXX", "#ops-alerts"));
        registry.register(new FileOperationsPlugin());
        registry.register(auditPlugin);

        // ============================================================
        // STEP 4: Initialize + Start all plugins
        // ============================================================
        System.out.println("\n========== Plugin Initialization ==========\n");
        registry.initializeAll();

        System.out.println("\n========== Plugin Startup ==========\n");
        registry.startAll();

        registry.printStatus();

        // ============================================================
        // STEP 5: Run workflows — plugins enhance execution
        // ============================================================

        // Workflow 1: Uses built-in tasks + pipeline + Slack + audit
        WorkflowDefinition etl = WorkflowBuilder.create()
                .name("Data Sync Pipeline")
                .cronTrigger("0 */6 * * *")
                .addHttpGet("Fetch Updates", "https://api.partner.com/changes")
                .addTransformTask("Merge Changes", "raw_updates", "merge")
                .addDatabaseTask("Apply to DB", "CALL sp_sync('${timestamp}')")
                .sequential()
                .build();

        System.out.println("\n========== WORKFLOW 1: Data Sync (built-in tasks) ==========\n");
        engine.registerWorkflow(etl);
        engine.executeWorkflow(etl.getId());

        // Workflow 2: Uses plugin-provided task types (ftp, file_copy)
        // These types didn't exist before FileOperationsPlugin was loaded!
        WorkflowDefinition fileWorkflow = WorkflowBuilder.create()
                .name("File Export Pipeline")
                .webhookTrigger("/api/export")
                .addDatabaseTask("Export to CSV", "COPY sales TO '/tmp/sales.csv'")
                .addTask("Copy to Staging", "file_copy", Map.of(
                        "source", "/tmp/sales.csv",
                        "destination", "/staging/sales.csv"))
                .addTask("Upload to Partner", "ftp", Map.of(
                        "host", "ftp.partner.com",
                        "file", "/staging/sales.csv",
                        "direction", "upload"))
                .addTask("Cleanup Temp", "file_delete", Map.of(
                        "path", "/tmp/sales.csv"))
                .sequential()
                .build();

        System.out.println("\n========== WORKFLOW 2: File Export (plugin tasks) ==========\n");
        engine.registerWorkflow(fileWorkflow);
        engine.executeWorkflow(fileWorkflow.getId());

        // Workflow 3: Fails — triggers Slack notification via plugin
        WorkflowDefinition bad = WorkflowBuilder.create()
                .name("Failing Job")
                .manualTrigger()
                .addTask("Mystery Task", "nonexistent_type", Map.of())
                .sequential()
                .build();

        System.out.println("\n========== WORKFLOW 3: Failure (Slack alert) ==========\n");
        engine.registerWorkflow(bad);
        engine.executeWorkflow(bad.getId()); // Pipeline validation will catch this

        // ============================================================
        // STEP 6: Reports + Shutdown
        // ============================================================
        System.out.println("\n========== Metrics ==========\n");
        metrics.printReport();

        System.out.println("\n========== Audit Trail ==========\n");
        auditPlugin.printAuditTrail();

        System.out.println("\n========== Shutdown ==========\n");
        registry.stopAll();
        registry.printStatus();
    }
}