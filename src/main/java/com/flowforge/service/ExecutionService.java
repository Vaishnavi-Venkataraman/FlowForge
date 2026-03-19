package com.flowforge.service;

import com.flowforge.engine.WorkflowEngine;
import com.flowforge.exception.FlowForgeException;
import com.flowforge.model.WorkflowDefinition;

import java.util.Map;

public class ExecutionService {

    private static final String SERVICE_NAME = "ExecutionService";
    private static final String CHANNEL_REQUESTS = "execution.requests";

    private final WorkflowEngine engine;
    private final ServiceBus bus;

    public ExecutionService(WorkflowEngine engine, ServiceBus bus) {
        this.engine = engine;
        this.bus = bus;

        // Subscribe to execution requests from other services
        bus.subscribe(CHANNEL_REQUESTS, this::handleExecutionRequest);

        System.out.println("[" + SERVICE_NAME + "] Started. Listening on: " + CHANNEL_REQUESTS);
    }

    /**
     * Handles incoming execution requests from the bus.
     */
    private void handleExecutionRequest(ServiceMessage message) {
        String workflowId = message.getPayloadValue("workflowId", null);
        String workflowName = message.getPayloadValue("workflowName", "unknown");

        System.out.println("[" + SERVICE_NAME + "] Received execution request: " + workflowName);

        if (workflowId == null) {
            bus.publish("execution.errors", ServiceMessage.of("EXECUTION_ERROR", SERVICE_NAME,
                    Map.of("error", "Missing workflowId", "source", message.getSourceService())));
            return;
        }

        try {
            engine.executeWorkflow(workflowId);

            // Publish success to other services
            bus.publish("notifications", ServiceMessage.of("WORKFLOW_COMPLETED", SERVICE_NAME,
                    Map.of("workflowName", workflowName, "workflowId", workflowId)));
            bus.publish("analytics", ServiceMessage.of("EXECUTION_COMPLETE", SERVICE_NAME,
                    Map.of("workflowName", workflowName, "workflowId", workflowId)));
            bus.publish("audit", ServiceMessage.of("EXECUTION_SUCCESS", SERVICE_NAME,
                    Map.of("workflowName", workflowName)));

            // Reply if requested
            if (message.getReplyChannel() != null) {
                bus.publish(message.getReplyChannel(), ServiceMessage.of(
                        "EXECUTION_RESULT", SERVICE_NAME,
                        Map.of("status", "COMPLETED", "workflowId", workflowId)));
            }

        } catch (FlowForgeException e) {
            bus.publish("notifications", ServiceMessage.of("WORKFLOW_FAILED", SERVICE_NAME,
                    Map.of("workflowName", workflowName, "error", e.getMessage())));
            bus.publish("analytics", ServiceMessage.of("EXECUTION_FAILED", SERVICE_NAME,
                    Map.of("workflowName", workflowName, "error", e.getMessage())));
            bus.publish("audit", ServiceMessage.of("EXECUTION_FAILURE", SERVICE_NAME,
                    Map.of("workflowName", workflowName, "error", e.getMessage())));
        }
    }

    /**
     * Direct registration (would be via API in real microservices).
     */
    public String registerWorkflow(WorkflowDefinition workflow) {
        return engine.registerWorkflow(workflow);
    }
}