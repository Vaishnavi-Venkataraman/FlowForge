package com.flowforge;
import com.flowforge.web.UserStore;
import com.flowforge.web.WebServer;
import com.flowforge.web.WorkflowStore;
import com.flowforge.web.handler.AuthHandler;
import com.flowforge.web.handler.StaticFileHandler;
import com.flowforge.web.handler.WorkflowApiHandler;

public class Main {

    private static final int PORT = 8080;

    public static void main(String[] args) throws Exception {
        System.out.println("========== FlowForge Web UI ==========");
        System.out.println();

        // Core engine (Facade + Singleton)
        FlowForgeFacade flowforge = FlowForgeFacade.getInstance();

        // Register external services for demo
        flowforge
                .addRestService("crm-api", "CRM REST API", "https://api.crm.com")
                .addSoapService("erp-legacy", "ERP SOAP", "https://erp.legacy.com/ws?wsdl")
                .addCloudStorage("data-lake", "Data Lake", "us-east-1", "acct-123");

        // Install plugins
        flowforge
                .installPlugin(new com.flowforge.plugin.builtin.FileOperationsPlugin())
                .startPlugins();

        // Web layer
        UserStore userStore = new UserStore();
        WorkflowStore workflowStore = new WorkflowStore();

        WebServer server = new WebServer(PORT);
        server.registerHandler("/api/auth/", new AuthHandler(userStore));
        server.registerHandler("/api/workflows", new WorkflowApiHandler(userStore, workflowStore, flowforge));
        server.registerHandler("/", new StaticFileHandler());

        server.start();

        System.out.println();
        System.out.println("  ┌─────────────────────────────────────────┐");
        System.out.println("  │  FlowForge is running!                  │");
        System.out.println("  │  Open: http://localhost:" + PORT + "             │");
        System.out.println("  │  Press Ctrl+C to stop                   │");
        System.out.println("  └─────────────────────────────────────────┘");
        System.out.println();

        // Try to open browser automatically
        try {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                Runtime.getRuntime().exec("cmd /c start http://localhost:" + PORT);
            } else if (os.contains("mac")) {
                Runtime.getRuntime().exec("open http://localhost:" + PORT);
            } else {
                Runtime.getRuntime().exec("xdg-open http://localhost:" + PORT);
            }
        } catch (Exception e) {
            // Browser auto-open failed — user can open manually
        }

        // Keep running until Ctrl+C
        Thread.currentThread().join();
    }
}