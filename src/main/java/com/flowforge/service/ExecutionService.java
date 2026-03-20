package com.flowforge.service;
import java.util.logging.Logger;
import com.flowforge.engine.WorkflowEngine;
import com.flowforge.exception.FlowForgeException;
import com.flowforge.model.WorkflowDefinition;

import java.util.Map;

public class ExecutionService {
    private static final Logger LOGGER = Logger.getLogger(ExecutionService.class.getName());
    private static final String SERVICE_NAME = "ExecutionService";
    private static final String CHANNEL_REQUESTS = "execution.requests";
    private static final String KEY_WORKFLOW_ID = "workflowId";
    private static final String KEY_WORKFLOW_NAME = "workflowName";
    private static final String KEY_ERROR = "error";

    private final WorkflowEngine engine;
    private final ServiceBus bus;

    public ExecutionService(WorkflowEngine engine, ServiceBus bus) {
        this.engine = engine;
        this.bus = bus;

        // Subscribe to execution requests from other services
        bus.subscribe(CHANNEL_REQUESTS, this::handleExecutionRequest);

        LOGGER.info("[" + SERVICE_NAME + "] Started. Listening on: " + CHANNEL_REQUESTS);
    }

    private void handleExecutionRequest(ServiceMessage message) {
        String workflowId = message.getPayloadValue(KEY_WORKFLOW_ID, null);
        String workflowName = message.getPayloadValue(KEY_WORKFLOW_NAME, "unknown");

        LOGGER.info("[" + SERVICE_NAME + "] Received execution request: " + workflowName);

        if (workflowId == null) {
            bus.publish("execution.errors", ServiceMessage.of("EXECUTION_ERROR", SERVICE_NAME,
                    Map.of(KEY_ERROR, "Missing workflowId", "source", message.getSourceService())));
            return;
        }

        try {
            engine.executeWorkflow(workflowId);

            // Publish success
            bus.publish("notifications", ServiceMessage.of("WORKFLOW_COMPLETED", SERVICE_NAME,
                    Map.of(KEY_WORKFLOW_NAME, workflowName, KEY_WORKFLOW_ID, workflowId)));

            bus.publish("analytics", ServiceMessage.of("EXECUTION_COMPLETE", SERVICE_NAME,
                    Map.of(KEY_WORKFLOW_NAME, workflowName, KEY_WORKFLOW_ID, workflowId)));

            bus.publish("audit", ServiceMessage.of("EXECUTION_SUCCESS", SERVICE_NAME,
                    Map.of(KEY_WORKFLOW_NAME, workflowName)));

            // Reply if requested
            if (message.getReplyChannel() != null) {
                bus.publish(message.getReplyChannel(), ServiceMessage.of(
                        "EXECUTION_RESULT", SERVICE_NAME,
                        Map.of("status", "COMPLETED", KEY_WORKFLOW_ID, workflowId)));
            }

        } catch (FlowForgeException e) {
            bus.publish("notifications", ServiceMessage.of("WORKFLOW_FAILED", SERVICE_NAME,
                    Map.of(KEY_WORKFLOW_NAME, workflowName, KEY_ERROR, e.getMessage())));

            bus.publish("analytics", ServiceMessage.of("EXECUTION_FAILED", SERVICE_NAME,
                    Map.of(KEY_WORKFLOW_NAME, workflowName, KEY_ERROR, e.getMessage())));

            bus.publish("audit", ServiceMessage.of("EXECUTION_FAILURE", SERVICE_NAME,
                    Map.of(KEY_WORKFLOW_NAME, workflowName, KEY_ERROR, e.getMessage())));
        }
    }

    /**
     * Direct registration (would be via API in real microservices).
     */
    public String registerWorkflow(WorkflowDefinition workflow) {
        return engine.registerWorkflow(workflow);
    }
}