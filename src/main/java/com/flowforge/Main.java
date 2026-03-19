package com.flowforge;

import com.flowforge.adapter.*;
import com.flowforge.adapter.thirdparty.CloudStorageSDK;
import com.flowforge.adapter.thirdparty.RestApiClient;
import com.flowforge.adapter.thirdparty.SoapServiceClient;
import com.flowforge.engine.WorkflowBuilder;
import com.flowforge.engine.WorkflowEngine;
import com.flowforge.event.EventBus;
import com.flowforge.event.LoggingListener;
import com.flowforge.event.MetricsListener;
import com.flowforge.event.NotificationListener;
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

/**
 * Entry point — demonstrates Adapter pattern.
 *
 * THREE incompatible external services all used through ONE unified interface:
 *
 *   REST API   (RestApiClient)    → RestServiceAdapter    → ExternalService
 *   SOAP/XML   (SoapServiceClient) → SoapServiceAdapter   → ExternalService
 *   Cloud S3   (CloudStorageSDK)  → CloudStorageAdapter   → ExternalService
 *
 * Tasks call ExternalService.execute() — they don't know the protocol.
 * Adapters translate between our interface and each vendor's API.
 */
public class Main {

    public static void main(String[] args) {

        // ============================================================
        // CORE SETUP
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

        // ============================================================
        // PLUGIN SETUP
        // ============================================================
        PluginContext pluginCtx = new PluginContext(factory, eventBus, pipeline, engine::registerStrategy);
        PluginRegistry plugins = new PluginRegistry(pluginCtx);
        AuditTrailPlugin auditPlugin = new AuditTrailPlugin();
        plugins.register(new FileOperationsPlugin());
        plugins.register(new SlackNotificationPlugin("https://hooks.slack.com/XXX", "#ops"));
        plugins.register(auditPlugin);
        plugins.initializeAll();
        plugins.startAll();

        // ============================================================
        // ADAPTER SETUP — 3 incompatible services adapted to one interface
        // ============================================================
        System.out.println("\n========== Adapter Setup ==========\n");

        ServiceRegistry serviceRegistry = new ServiceRegistry();

        // 1. REST API adapter
        RestApiClient restClient = new RestApiClient("https://api.crm.example.com");
        serviceRegistry.register(new RestServiceAdapter("crm-api", "CRM REST API", restClient));

        // 2. SOAP adapter (legacy ERP)
        SoapServiceClient soapClient = new SoapServiceClient("https://erp.legacy.com/ws?wsdl");
        serviceRegistry.register(new SoapServiceAdapter("erp-legacy", "ERP SOAP Service", soapClient));

        // 3. Cloud Storage adapter (S3-compatible)
        CloudStorageSDK storageSDK = new CloudStorageSDK("us-east-1", "acct-12345");
        serviceRegistry.register(new CloudStorageAdapter("data-lake", "Cloud Data Lake", storageSDK));

        // Register the ExternalServiceTask type in the factory
        factory.registerTaskType("external_service",
                name -> new ExternalServiceTask(name, serviceRegistry));

        // Health check all services
        System.out.println();
        serviceRegistry.healthCheckAll();

        // ============================================================
        // WORKFLOW 1: REST API integration
        // ============================================================
        WorkflowDefinition restWorkflow = WorkflowBuilder.create()
                .name("CRM Data Sync (REST)")
                .cronTrigger("0 */2 * * *")
                .addTask("Fetch Customers", "external_service", Map.of(
                        "serviceId", "crm-api",
                        "operation", "GET",
                        "path", "/customers?active=true"))
                .addTransformTask("Normalize CRM Data", "customer_json", "flatten")
                .addDatabaseTask("Upsert to Local DB", "CALL sp_upsert_customers()")
                .sequential()
                .build();

        System.out.println("\n========== WORKFLOW 1: REST API Adapter ==========\n");
        engine.registerWorkflow(restWorkflow);
        engine.executeWorkflow(restWorkflow.getId());

        // ============================================================
        // WORKFLOW 2: SOAP/XML legacy integration
        // ============================================================
        WorkflowDefinition soapWorkflow = WorkflowBuilder.create()
                .name("ERP Invoice Sync (SOAP)")
                .cronTrigger("0 6 * * *")
                .addTask("Fetch Invoices", "external_service", Map.of(
                        "serviceId", "erp-legacy",
                        "operation", "GetPendingInvoices",
                        "department", "SALES",
                        "status", "PENDING"))
                .addTransformTask("Parse XML Response", "soap_xml", "xml_to_json")
                .addEmailTask("Send Invoice Report", "finance@company.com", "Pending Invoices Report")
                .sequential()
                .build();

        System.out.println("\n========== WORKFLOW 2: SOAP Adapter ==========\n");
        engine.registerWorkflow(soapWorkflow);
        engine.executeWorkflow(soapWorkflow.getId());

        // ============================================================
        // WORKFLOW 3: Cloud Storage integration
        // ============================================================
        WorkflowDefinition s3Workflow = WorkflowBuilder.create()
                .name("Data Lake Upload (Cloud Storage)")
                .webhookTrigger("/api/data/upload")
                .addDatabaseTask("Export Report", "COPY report TO '/tmp/report.csv'")
                .addTask("Upload to Data Lake", "external_service", Map.of(
                        "serviceId", "data-lake",
                        "operation", "UPLOAD",
                        "bucket", "analytics-raw",
                        "key", "reports/${timestamp}/report.csv",
                        "content", "csv-data-here"))
                .sequential()
                .build();

        System.out.println("\n========== WORKFLOW 3: Cloud Storage Adapter ==========\n");
        engine.registerWorkflow(s3Workflow);
        engine.executeWorkflow(s3Workflow.getId());

        // ============================================================
        // WORKFLOW 4: Multi-protocol — one workflow using ALL adapters
        // ============================================================
        WorkflowDefinition multiProtocol = WorkflowBuilder.create()
                .name("Full Integration Pipeline")
                .manualTrigger()
                .addTask("Fetch from CRM (REST)", "external_service", Map.of(
                        "serviceId", "crm-api", "operation", "GET", "path", "/deals"))
                .addTask("Get ERP Data (SOAP)", "external_service", Map.of(
                        "serviceId", "erp-legacy", "operation", "GetDealFinancials",
                        "dealId", "DEAL-789"))
                .addTransformTask("Merge Data", "combined", "merge")
                .addTask("Store in Lake (S3)", "external_service", Map.of(
                        "serviceId", "data-lake", "operation", "UPLOAD",
                        "bucket", "merged-data", "key", "deals/latest.json",
                        "content", "merged-json-data"))
                .addEmailTask("Notify Team", "team@company.com", "Integration pipeline complete")
                .sequential()
                .build();

        System.out.println("\n========== WORKFLOW 4: Multi-Protocol (REST+SOAP+S3) ==========\n");
        engine.registerWorkflow(multiProtocol);
        engine.executeWorkflow(multiProtocol.getId());

        // ============================================================
        // REPORTS
        // ============================================================
        System.out.println("\n========== Metrics ==========\n");
        metrics.printReport();

        System.out.println("\n========== Audit Trail ==========\n");
        auditPlugin.printAuditTrail();

        System.out.println("\n========== Shutdown ==========\n");
        plugins.stopAll();
    }
}