package com.flowforge;

import com.flowforge.adapter.*;
import com.flowforge.adapter.thirdparty.CloudStorageSDK;
import com.flowforge.adapter.thirdparty.RestApiClient;
import com.flowforge.adapter.thirdparty.SoapServiceClient;
import com.flowforge.engine.WorkflowBuilder;
import com.flowforge.engine.WorkflowEngine;
import com.flowforge.engine.strategy.ConditionalStrategy;
import com.flowforge.engine.strategy.ParallelStrategy;
import com.flowforge.event.EventBus;
import com.flowforge.event.LoggingListener;
import com.flowforge.model.WorkflowDefinition;
import com.flowforge.pipeline.*;
import com.flowforge.plugin.PluginContext;
import com.flowforge.plugin.PluginRegistry;
import com.flowforge.plugin.builtin.FileOperationsPlugin;
import com.flowforge.plugin.builtin.SlackNotificationPlugin;
import com.flowforge.service.*;
import com.flowforge.task.TaskFactory;
import com.flowforge.task.decorator.LoggingDecorator;
import com.flowforge.task.decorator.RetryDecorator;
import com.flowforge.task.decorator.TimeoutDecorator;
import java.util.Map;
import java.util.Set;

public class Main {

    public static void main(String[] args) {

        System.out.println("========== System Startup ==========\n");
        ServiceBus serviceBus = new ServiceBus();
        EventBus eventBus = new EventBus();
        eventBus.subscribeAll(new LoggingListener()); // Workflow-level event logging

        TaskFactory factory = new TaskFactory();
        factory.setGlobalDecorator(task ->
                new LoggingDecorator(new RetryDecorator(new TimeoutDecorator(task, 10000), 1, 50)));

        ServiceRegistry serviceRegistry = new ServiceRegistry();
        serviceRegistry.register(new RestServiceAdapter("crm-api", "CRM REST API",
                new RestApiClient("https://api.crm.com")));
        serviceRegistry.register(new SoapServiceAdapter("erp-legacy", "ERP SOAP Service",
                new SoapServiceClient("https://erp.legacy.com/ws?wsdl")));
        serviceRegistry.register(new CloudStorageAdapter("data-lake", "Cloud Data Lake",
                new CloudStorageSDK("us-east-1", "acct-123")));
        factory.registerTaskType("external_service",
                name -> new ExternalServiceTask(name, serviceRegistry));

        Pipeline pipeline = new Pipeline()
                .addHandler(new ValidationHandler(factory))
                .addHandler(new AuthorizationHandler("admin", Set.of("Restricted Workflow")))
                .addHandler(new RateLimitHandler(3000))
                .addHandler(new TransformHandler());

        WorkflowEngine engine = new WorkflowEngine(factory, eventBus);
        engine.setPipeline(pipeline);
        engine.registerStrategy(new ParallelStrategy());
        engine.registerStrategy(new ConditionalStrategy(
                config -> !config.getType().equals("delay"),
                "skip-delays"
        ));

        PluginContext pluginCtx = new PluginContext(factory, eventBus, pipeline, engine::registerStrategy);
        PluginRegistry plugins = new PluginRegistry(pluginCtx);
        plugins.register(new FileOperationsPlugin());
        plugins.register(new SlackNotificationPlugin("https://hooks.slack.com/XXX", "#ops"));
        plugins.initializeAll();
        plugins.startAll();

        System.out.println("\n========== Services ==========\n");
        ExecutionService executionService = new ExecutionService(engine, serviceBus);
        NotificationService notificationService = new NotificationService(serviceBus);
        AnalyticsService analyticsService = new AnalyticsService(serviceBus);
        AuditService auditService = new AuditService(serviceBus);
        TriggerService triggerService = new TriggerService(serviceBus);


        System.out.println("\n========== Register Workflows ==========\n");

        // WF1: Sequential ETL (uses REST adapter)
        WorkflowDefinition etl = WorkflowBuilder.create()
                .name("ETL Pipeline")
                .cronTrigger("0 2 * * *")
                .addTask("Fetch from CRM", "external_service", Map.of(
                        "serviceId", "crm-api", "operation", "GET", "path", "/customers"))
                .addTransformTask("Normalize", "raw", "normalize")
                .addDatabaseTask("Load", "INSERT INTO warehouse VALUES('${timestamp}')")
                .sequential()
                .build();
        String etlId = executionService.registerWorkflow(etl);
        triggerService.registerTrigger(etlId, etl.getName(), "CRON", "0 2 * * *");

        // WF2: Parallel notifications
        WorkflowDefinition notify = WorkflowBuilder.create()
                .name("Order Notifications")
                .webhookTrigger("/api/orders/new")
                .addEmailTask("Email Customer", "customer@shop.com", "Order Confirmed!")
                .addTask("Update ERP (SOAP)", "external_service", Map.of(
                        "serviceId", "erp-legacy", "operation", "CreateOrder",
                        "orderId", "ORD-999", "amount", "150.00"))
                .addTask("Archive to Lake", "external_service", Map.of(
                        "serviceId", "data-lake", "operation", "UPLOAD",
                        "bucket", "orders", "key", "latest.json", "content", "order-data"))
                .parallel()
                .build();
        String notifyId = executionService.registerWorkflow(notify);
        triggerService.registerTrigger(notifyId, notify.getName(), "WEBHOOK", "/api/orders/new");

        // WF3: Conditional (skips delay tasks) + plugin task types
        WorkflowDefinition fileJob = WorkflowBuilder.create()
                .name("File Export (Conditional)")
                .manualTrigger()
                .addDatabaseTask("Export CSV", "COPY sales TO '/tmp/sales.csv'")
                .addDelayTask("Wait for flush", 2)
                .addTask("FTP Upload", "ftp", Map.of(
                        "host", "ftp.partner.com", "file", "sales.csv"))
                .strategy("conditional(skip-delays)")
                .build();
        String fileId = executionService.registerWorkflow(fileJob);
        triggerService.registerTrigger(fileId, fileJob.getName(), "MANUAL", "");

        // WF4: Will fail (unknown task type) — tests error path
        WorkflowDefinition bad = WorkflowBuilder.create()
                .name("Failing Workflow")
                .manualTrigger()
                .addTask("Bad Task", "nonexistent", Map.of())
                .sequential()
                .build();
        String badId = executionService.registerWorkflow(bad);
        triggerService.registerTrigger(badId, bad.getName(), "MANUAL", "");

        System.out.println("\n========== Scenario 1: Cron → Sequential ETL (REST) ==========\n");
        triggerService.fireTrigger(etlId);

        System.out.println("\n========== Scenario 2: Webhook → Parallel (SOAP + S3) ==========\n");
        triggerService.simulateWebhook("/api/orders/new");

        System.out.println("\n========== Scenario 3: Manual → Conditional (skip delays) ==========\n");
        triggerService.fireTrigger(fileId);

        System.out.println("\n========== Scenario 4: Failure Path ==========\n");
        triggerService.fireTrigger(badId);


        System.out.println("\n========== Service Health ==========\n");
        serviceRegistry.healthCheckAll();

        System.out.println("\n========== Dashboards ==========\n");
        notificationService.printSummary();
        System.out.println();
        analyticsService.printDashboard();
        System.out.println();
        auditService.printAuditLog();


        System.out.println("\n========== Shutdown ==========\n");
        plugins.stopAll();
        plugins.printStatus();
    }
}