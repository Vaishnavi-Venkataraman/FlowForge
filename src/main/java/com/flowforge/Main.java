package com.flowforge;

import com.flowforge.engine.WorkflowBuilder;
import com.flowforge.model.WorkflowDefinition;
import com.flowforge.plugin.builtin.FileOperationsPlugin;

import java.util.Map;

public class Main {
        public static void main(String[] args) {

        // One line to get the entire platform — Singleton + Facade
        FlowForgeFacade flowforge = FlowForgeFacade.getInstance();

        System.out.println("========== Setup ==========\n");

        flowforge
                .addRestService("crm-api", "CRM REST API", "https://api.crm.com")
                .addSoapService("erp-legacy", "ERP SOAP", "https://erp.legacy.com/ws?wsdl")
                .addCloudStorage("data-lake", "Data Lake", "us-east-1", "acct-123")
                .installPlugin(new FileOperationsPlugin())
                .startPlugins();

        // WORKFLOW 1: Sequential ETL (REST adapter)
        WorkflowDefinition etl = WorkflowBuilder.create()
                .name("ETL Pipeline")
                .cronTrigger("0 2 * * *")
                .addTask("Fetch from CRM", "external_service", Map.of(
                        "serviceId", "crm-api", "operation", "GET", "path", "/customers"))
                .addTransformTask("Normalize", "raw", "normalize")
                .addDatabaseTask("Load", "INSERT INTO warehouse VALUES('${timestamp}')")
                .sequential()
                .build();
        String etlId = flowforge.registerWorkflow(etl);

        // WORKFLOW 2: Parallel notifications (SOAP + Cloud Storage)
        WorkflowDefinition notify = WorkflowBuilder.create()
                .name("Order Notifications")
                .webhookTrigger("/api/orders/new")
                .addEmailTask("Email Customer", "customer@shop.com", "Order Confirmed!")
                .addTask("Update ERP", "external_service", Map.of(
                        "serviceId", "erp-legacy", "operation", "CreateOrder",
                        "orderId", "ORD-999", "amount", "150.00"))
                .addTask("Archive to Lake", "external_service", Map.of(
                        "serviceId", "data-lake", "operation", "UPLOAD",
                        "bucket", "orders", "key", "latest.json", "content", "order-data"))
                .parallel()
                .build();
        flowforge.registerWorkflow(notify);

        // WORKFLOW 3: Conditional + plugin task types
        WorkflowDefinition fileJob = WorkflowBuilder.create()
                .name("File Export")
                .manualTrigger()
                .addDatabaseTask("Export CSV", "COPY sales TO '/tmp/sales.csv'")
                .addDelayTask("Wait", 2)
                .addTask("FTP Upload", "ftp", Map.of("host", "ftp.partner.com", "file", "sales.csv"))
                .strategy("conditional(skip-delays)")
                .build();
        String fileId = flowforge.registerWorkflow(fileJob);

        System.out.println("\n========== Execute via Triggers ==========\n");

        System.out.println("--- Cron trigger → ETL ---\n");
        flowforge.fireTrigger(etlId);

        System.out.println("\n--- Webhook → Order Notifications ---\n");
        flowforge.simulateWebhook("/api/orders/new");

        System.out.println("\n--- Manual trigger → File Export ---\n");
        flowforge.fireTrigger(fileId);

        System.out.println("\n========== Dashboards ==========\n");
        flowforge.healthCheck();
        System.out.println();
        flowforge.printDashboards();

        System.out.println("\n========== Shutdown ==========\n");
        flowforge.shutdown();
    }
}