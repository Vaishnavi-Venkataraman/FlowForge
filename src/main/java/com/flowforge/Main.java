package com.flowforge;

import com.flowforge.plugin.builtin.FileOperationsPlugin;
import com.flowforge.web.UserStore;
import com.flowforge.web.WebServer;
import com.flowforge.web.WorkflowStore;
import com.flowforge.web.handler.AuthHandler;
import com.flowforge.web.handler.StaticFileHandler;
import com.flowforge.web.handler.WorkflowApiHandler;

public class Main {

    private static final int PORT = 8080;

    public static void main(String[] args) throws Exception {
        System.out.println("========== FlowForge ==========");

        // Core engine
        FlowForgeFacade flowforge = FlowForgeFacade.getInstance();
        flowforge
                .addRestService("crm-api", "CRM REST API", "https://api.crm.com")
                .addSoapService("erp-legacy", "ERP SOAP", "https://erp.legacy.com/ws?wsdl")
                .addCloudStorage("data-lake", "Data Lake", "us-east-1", "acct-123")
                .installPlugin(new FileOperationsPlugin())
                .startPlugins();

        // Data stores (file-persisted — survives restarts)
        UserStore userStore = new UserStore();
        WorkflowStore workflowStore = new WorkflowStore();

        // API handlers
        WorkflowApiHandler workflowHandler = new WorkflowApiHandler(userStore, workflowStore, flowforge);
        workflowHandler.reloadPersistedWorkflows();

        // Web server
        WebServer server = new WebServer(PORT);
        server.registerHandler("/api/auth/", new AuthHandler(userStore));
        server.registerHandler("/api/workflows", workflowHandler);
        server.registerHandler("/", new StaticFileHandler());
        server.start();

        System.out.println();
        System.out.println("  +------------------------------------------+");
        System.out.println("  |  FlowForge is running!                   |");
        System.out.println("  |  Open: http://localhost:" + PORT + "              |");
        System.out.println("  |  Data saved in: data/                    |");
        System.out.println("  |  Press Ctrl+C to stop                    |");
        System.out.println("  +------------------------------------------+");
        System.out.println();

        // Auto-open browser
            try {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            new ProcessBuilder("cmd", "/c", "start", "http://localhost:" + PORT).start();
        } else if (os.contains("mac")) {
            new ProcessBuilder("open", "http://localhost:" + PORT).start();
        } else {
            new ProcessBuilder("xdg-open", "http://localhost:" + PORT).start();
        }

    } catch (Exception ignored) {
        // Browser auto-open is best-effort; failure is not critical
    }

        Thread.currentThread().join();
    }
}