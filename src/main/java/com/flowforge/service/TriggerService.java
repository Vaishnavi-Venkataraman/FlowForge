package com.flowforge.service;

import java.util.HashMap;
import java.util.Map;

public class TriggerService {

    private static final String SERVICE_NAME = "TriggerService";

    private final ServiceBus bus;
    private final Map<String, WorkflowTriggerInfo> registeredTriggers = new HashMap<>();

    public TriggerService(ServiceBus bus) {
        this.bus = bus;
        System.out.println("[" + SERVICE_NAME + "] Started. Publishes to: execution.requests, notifications");
    }

    /**
     * Registers a workflow trigger. In production, this info would come
     * from a shared database or API call, not direct method invocation.
     */
    public void registerTrigger(String workflowId, String workflowName,
                                 String triggerType, String triggerValue) {
        registeredTriggers.put(workflowId,
                new WorkflowTriggerInfo(workflowId, workflowName, triggerType, triggerValue));
        System.out.println("[" + SERVICE_NAME + "] Registered trigger: "
                + triggerType + "(" + triggerValue + ") → " + workflowName);
    }

    /**
     * Simulates a trigger firing — publishes execution request to the bus.
     * In production: cron scheduler fires, webhook endpoint hit, file detected.
     */
    public void fireTrigger(String workflowId) {
        WorkflowTriggerInfo info = registeredTriggers.get(workflowId);
        if (info == null) {
            System.out.println("[" + SERVICE_NAME + "] Unknown workflow ID: " + workflowId);
            return;
        }

        System.out.println("[" + SERVICE_NAME + "] Trigger fired: "
                + info.triggerType + "(" + info.triggerValue + ") → " + info.workflowName);

        // Notify about trigger firing
        bus.publish("notifications", ServiceMessage.of("TRIGGER_FIRED", SERVICE_NAME,
                Map.of("workflowName", info.workflowName,
                        "triggerType", info.triggerType,
                        "triggerValue", info.triggerValue)));

        // Request execution — TriggerService does NOT execute, it requests
        bus.publish("execution.requests", ServiceMessage.of("EXECUTE", SERVICE_NAME,
                Map.of("workflowId", info.workflowId,
                        "workflowName", info.workflowName,
                        "triggeredBy", info.triggerType)));

        // Publish to analytics
        bus.publish("analytics", ServiceMessage.of("TRIGGER_FIRED", SERVICE_NAME,
                Map.of("workflowName", info.workflowName, "triggerType", info.triggerType)));
    }

    /**
     * Simulates a webhook being received.
     */
    public void simulateWebhook(String path) {
        for (WorkflowTriggerInfo info : registeredTriggers.values()) {
            if ("WEBHOOK".equals(info.triggerType) && info.triggerValue.equals(path)) {
                System.out.println("[" + SERVICE_NAME + "] Webhook matched: " + path);
                fireTrigger(info.workflowId);
                return;
            }
        }
        System.out.println("[" + SERVICE_NAME + "] No workflow registered for webhook: " + path);
    }

    private record WorkflowTriggerInfo(String workflowId, String workflowName,
                                        String triggerType, String triggerValue) {
    }
}