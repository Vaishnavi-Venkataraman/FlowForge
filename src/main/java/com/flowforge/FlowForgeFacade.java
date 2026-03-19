package com.flowforge;

import com.flowforge.adapter.*;
import com.flowforge.adapter.thirdparty.CloudStorageSDK;
import com.flowforge.adapter.thirdparty.RestApiClient;
import com.flowforge.adapter.thirdparty.SoapServiceClient;
import com.flowforge.engine.WorkflowEngine;
import com.flowforge.engine.strategy.ConditionalStrategy;
import com.flowforge.engine.strategy.ParallelStrategy;
import com.flowforge.event.EventBus;
import com.flowforge.event.LoggingListener;
import com.flowforge.model.WorkflowDefinition;
import com.flowforge.pipeline.*;
import com.flowforge.plugin.Plugin;
import com.flowforge.plugin.PluginContext;
import com.flowforge.plugin.PluginRegistry;
import com.flowforge.service.*;
import com.flowforge.task.Task;
import com.flowforge.task.TaskFactory;
import com.flowforge.task.decorator.LoggingDecorator;
import com.flowforge.task.decorator.RetryDecorator;
import com.flowforge.task.decorator.TimeoutDecorator;

import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

public class FlowForgeFacade {

    // --- Singleton via Holder pattern (lazy, thread-safe) ---
    private static class Holder {
        private static final FlowForgeFacade INSTANCE = new FlowForgeFacade();
    }

    public static FlowForgeFacade getInstance() {
        return Holder.INSTANCE;
    }

    // --- Internal subsystems (hidden from users) ---
    private final EventBus eventBus;
    private final TaskFactory taskFactory;
    private final Pipeline pipeline;
    private final WorkflowEngine engine;
    private final ServiceRegistry serviceRegistry;
    private final ServiceBus serviceBus;
    private final PluginRegistry pluginRegistry;

    // Microservices
    private final ExecutionService executionService;
    private final NotificationService notificationService;
    private final AnalyticsService analyticsService;
    private final AuditService auditService;
    private final TriggerService triggerService;

    private FlowForgeFacade() {
        // Event system
        this.eventBus = new EventBus();
        eventBus.subscribeAll(new LoggingListener());

        // Task factory with decorator chain
        this.taskFactory = new TaskFactory();
        taskFactory.setGlobalDecorator(task ->
                new LoggingDecorator(new RetryDecorator(new TimeoutDecorator(task, 10000), 1, 50)));

        // External service adapters
        this.serviceRegistry = new ServiceRegistry();
        taskFactory.registerTaskType("external_service",
                name -> new ExternalServiceTask(name, serviceRegistry));

        // Pipeline
        this.pipeline = new Pipeline()
                .addHandler(new ValidationHandler(taskFactory))
                .addHandler(new AuthorizationHandler("admin", Set.of()))
                .addHandler(new RateLimitHandler(2000))
                .addHandler(new TransformHandler());

        // Engine with strategies
        this.engine = new WorkflowEngine(taskFactory, eventBus);
        engine.setPipeline(pipeline);
        engine.registerStrategy(new ParallelStrategy());
        engine.registerStrategy(new ConditionalStrategy(
                config -> !config.getType().equals("delay"), "skip-delays"));

        // Plugin system
        PluginContext pluginCtx = new PluginContext(
                taskFactory, eventBus, pipeline, engine::registerStrategy);
        this.pluginRegistry = new PluginRegistry(pluginCtx);

        // Microservices
        this.serviceBus = new ServiceBus();
        this.executionService = new ExecutionService(engine, serviceBus);
        this.notificationService = new NotificationService(serviceBus);
        this.analyticsService = new AnalyticsService(serviceBus);
        this.auditService = new AuditService(serviceBus);
        this.triggerService = new TriggerService(serviceBus);
    }

    // ===== SIMPLE PUBLIC API (what users see) =====

    /**
     * Register a workflow and return its ID.
     */
    public String registerWorkflow(WorkflowDefinition workflow) {
        String id = executionService.registerWorkflow(workflow);
        if (workflow.getTrigger() != null) {
            triggerService.registerTrigger(id, workflow.getName(),
                    workflow.getTrigger().getType().name(),
                    workflow.getTrigger().getValue());
        }
        return id;
    }

    /**
     * Execute a workflow directly by ID.
     */
    public void executeWorkflow(String workflowId) {
        engine.executeWorkflow(workflowId);
    }

    /**
     * Fire a trigger (simulates cron/webhook/manual trigger).
     */
    public void fireTrigger(String workflowId) {
        triggerService.fireTrigger(workflowId);
    }

    /**
     * Simulate an incoming webhook.
     */
    public void simulateWebhook(String path) {
        triggerService.simulateWebhook(path);
    }

    /**
     * Register a REST API as an external service.
     */
    public FlowForgeFacade addRestService(String serviceId, String name, String baseUrl) {
        serviceRegistry.register(new RestServiceAdapter(serviceId, name,
                new RestApiClient(baseUrl)));
        return this;
    }

    /**
     * Register a SOAP service as an external service.
     */
    public FlowForgeFacade addSoapService(String serviceId, String name, String wsdlUrl) {
        serviceRegistry.register(new SoapServiceAdapter(serviceId, name,
                new SoapServiceClient(wsdlUrl)));
        return this;
    }

    /**
     * Register a cloud storage service.
     */
    public FlowForgeFacade addCloudStorage(String serviceId, String name, String region, String account) {
        serviceRegistry.register(new CloudStorageAdapter(serviceId, name,
                new CloudStorageSDK(region, account)));
        return this;
    }

    /**
     * Register a custom task type.
     */
    public FlowForgeFacade addTaskType(String type, Function<String, Task> creator) {
        taskFactory.registerTaskType(type, creator);
        return this;
    }

    /**
     * Install a plugin.
     */
    public FlowForgeFacade installPlugin(Plugin plugin) {
        pluginRegistry.register(plugin);
        return this;
    }

    /**
     * Initialize and start all installed plugins.
     */
    public FlowForgeFacade startPlugins() {
        pluginRegistry.initializeAll();
        pluginRegistry.startAll();
        return this;
    }

    /**
     * Print all dashboards (notifications, analytics, audit).
     */
    public void printDashboards() {
        notificationService.printSummary();
        System.out.println();
        analyticsService.printDashboard();
        System.out.println();
        auditService.printAuditLog();
    }

    /**
     * Health check all external services.
     */
    public void healthCheck() {
        serviceRegistry.healthCheckAll();
    }

    /**
     * Shutdown plugins cleanly.
     */
    public void shutdown() {
        pluginRegistry.stopAll();
        System.out.println("[FlowForge] Shutdown complete.");
    }
}