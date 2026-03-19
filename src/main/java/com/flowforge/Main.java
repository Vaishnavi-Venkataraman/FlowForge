package com.flowforge;

import com.flowforge.adapter.*;
import com.flowforge.adapter.thirdparty.CloudStorageSDK;
import com.flowforge.adapter.thirdparty.RestApiClient;
import com.flowforge.engine.WorkflowBuilder;
import com.flowforge.engine.WorkflowEngine;
import com.flowforge.event.EventBus;
import com.flowforge.event.LoggingListener;
import com.flowforge.model.WorkflowDefinition;
import com.flowforge.pipeline.*;
import com.flowforge.plugin.PluginContext;
import com.flowforge.plugin.PluginRegistry;
import com.flowforge.plugin.builtin.FileOperationsPlugin;
import com.flowforge.service.*;
import com.flowforge.task.TaskFactory;
import com.flowforge.task.decorator.LoggingDecorator;

import java.util.Map;
import java.util.Set;

public class Main {

    public static void main(String[] args) {

        // ============================================================
        // INFRASTRUCTURE: ServiceBus (simulates Kafka/RabbitMQ)
        // ============================================================
        System.out.println("========== Infrastructure Setup ==========\n");
        ServiceBus serviceBus = new ServiceBus();

        // ============================================================
        // CORE ENGINE SETUP (internal to ExecutionService)
        // ============================================================
        EventBus eventBus = new EventBus();
        eventBus.subscribeAll(new LoggingListener());

        TaskFactory factory = new TaskFactory();
        factory.setGlobalDecorator(LoggingDecorator::new);

        // Adapter setup
        ServiceRegistry serviceRegistry = new ServiceRegistry();
        serviceRegistry.register(new RestServiceAdapter("crm-api", "CRM REST API",
                new RestApiClient("https://api.crm.com")));
        serviceRegistry.register(new CloudStorageAdapter("data-lake", "Data Lake",
                new CloudStorageSDK("us-east-1", "acct-123")));
        factory.registerTaskType("external_service",
                name -> new ExternalServiceTask(name, serviceRegistry));

        Pipeline pipeline = new Pipeline()
                .addHandler(new ValidationHandler(factory))
                .addHandler(new AuthorizationHandler("admin", Set.of()))
                .addHandler(new TransformHandler());

        WorkflowEngine engine = new WorkflowEngine(factory, eventBus);
        engine.setPipeline(pipeline);

        // Plugin setup
        PluginContext pluginCtx = new PluginContext(factory, eventBus, pipeline, engine::registerStrategy);
        PluginRegistry plugins = new PluginRegistry(pluginCtx);
        plugins.register(new FileOperationsPlugin());
        plugins.initializeAll();
        plugins.startAll();

        // ============================================================
        // MICROSERVICES: 5 independent services connected via ServiceBus
        // ============================================================
        System.out.println("\n========== Service Startup ==========\n");

        ExecutionService executionService = new ExecutionService(engine, serviceBus);
        NotificationService notificationService = new NotificationService(serviceBus);
        AnalyticsService analyticsService = new AnalyticsService(serviceBus);
        AuditService auditService = new AuditService(serviceBus);
        TriggerService triggerService = new TriggerService(serviceBus);

        // ============================================================
        // REGISTER WORKFLOWS
        // ============================================================
        System.out.println("\n========== Register Workflows ==========\n");

        WorkflowDefinition etl = WorkflowBuilder.create()
                .name("ETL Pipeline")
                .cronTrigger("0 2 * * *")
                .addHttpGet("Extract", "https://api.source.com/data")
                .addTransformTask("Transform", "raw", "normalize")
                .addDatabaseTask("Load", "INSERT INTO warehouse SELECT * FROM staging")
                .sequential()
                .build();
        String etlId = executionService.registerWorkflow(etl);
        triggerService.registerTrigger(etlId, etl.getName(), "CRON", "0 2 * * *");

        WorkflowDefinition webhook = WorkflowBuilder.create()
                .name("Webhook Handler")
                .webhookTrigger("/api/orders/new")
                .addTask("Fetch Order", "external_service", Map.of(
                        "serviceId", "crm-api", "operation", "GET", "path", "/orders/latest"))
                .addEmailTask("Confirm Order", "customer@shop.com", "Order Received!")
                .addTask("Archive to Lake", "external_service", Map.of(
                        "serviceId", "data-lake", "operation", "UPLOAD",
                        "bucket", "orders", "key", "latest.json", "content", "order-data"))
                .sequential()
                .build();
        String webhookId = executionService.registerWorkflow(webhook);
        triggerService.registerTrigger(webhookId, webhook.getName(), "WEBHOOK", "/api/orders/new");

        WorkflowDefinition failWorkflow = WorkflowBuilder.create()
                .name("Failing Workflow")
                .manualTrigger()
                .addTask("Bad Task", "nonexistent", Map.of())
                .sequential()
                .build();
        String failId = executionService.registerWorkflow(failWorkflow);
        triggerService.registerTrigger(failId, failWorkflow.getName(), "MANUAL", "");

        // ============================================================
        // SIMULATE: Triggers fire → services react via bus
        // ============================================================
        System.out.println("\n========== Scenario 1: Cron Trigger Fires ==========\n");
        triggerService.fireTrigger(etlId);

        System.out.println("\n========== Scenario 2: Webhook Received ==========\n");
        triggerService.simulateWebhook("/api/orders/new");

        System.out.println("\n========== Scenario 3: Manual Trigger (will fail) ==========\n");
        triggerService.fireTrigger(failId);

        // ============================================================
        // SERVICE DASHBOARDS — each service reports independently
        // ============================================================
        System.out.println("\n========== Service Dashboards ==========\n");
        notificationService.printSummary();
        System.out.println();
        analyticsService.printDashboard();
        System.out.println();
        auditService.printAuditLog();
    }
}