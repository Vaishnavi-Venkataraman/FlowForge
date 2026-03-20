package com.flowforge;

import java.util.logging.Logger;

import com.flowforge.plugin.builtin.FileOperationsPlugin;
import com.flowforge.web.UserStore;
import com.flowforge.web.WebServer;
import com.flowforge.web.WorkflowStore;
import com.flowforge.web.handler.AuthHandler;
import com.flowforge.web.handler.StaticFileHandler;
import com.flowforge.web.handler.WorkflowApiHandler;

public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    private static final int PORT = 8080;
    private static final String BASE_URL = "http://localhost:" + PORT;

    public static void main(String[] args) throws Exception {

        LOGGER.info("========== FlowForge ==========");

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
        WorkflowApiHandler workflowHandler =
                new WorkflowApiHandler(userStore, workflowStore, flowforge);
        workflowHandler.reloadPersistedWorkflows();

        // Web server
        WebServer server = new WebServer(PORT);
        server.registerHandler("/api/auth/", new AuthHandler(userStore));
        server.registerHandler("/api/workflows", workflowHandler);
        server.registerHandler("/", new StaticFileHandler());
        server.start();

        LOGGER.info(() -> "  +------------------------------------------+");
        LOGGER.info("  |  FlowForge is running!                   |");
        LOGGER.info(() -> "  |  Open: " + BASE_URL + "              |");
        LOGGER.info("  |  Data saved in: data/                    |");
        LOGGER.info(() -> "  |  Press Ctrl+C to stop                    |");
        LOGGER.info(() -> "  +------------------------------------------+");

        // Auto-open browser
        try {
            String os = System.getProperty("os.name").toLowerCase();

            if (os.contains("win")) {
                new ProcessBuilder("cmd", "/c", "start", BASE_URL).start();
            } else if (os.contains("mac")) {
                new ProcessBuilder("open", BASE_URL).start();
            } else {
                new ProcessBuilder("xdg-open", BASE_URL).start();
            }

        } catch (Exception ignored) {
            // Browser auto-open is best-effort; failure is not critical
        }

        // Keep server running
        Thread.currentThread().join();
    }
}