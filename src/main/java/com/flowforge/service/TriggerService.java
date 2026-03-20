package com.flowforge.service;
import java.util.logging.Logger;
import java.util.HashMap;
import java.util.Map;

public class TriggerService {

    private static final Logger LOGGER = Logger.getLogger(TriggerService.class.getName());
    private static final String SERVICE_NAME = "TriggerService";
    private static final String KEY_WORKFLOW_NAME = "workflowName";
    private final ServiceBus bus;
    private final Map<String, WorkflowTriggerInfo> registeredTriggers = new HashMap<>();

    public TriggerService(ServiceBus bus) {
        this.bus = bus;
        LOGGER.info(() -> "[" + SERVICE_NAME + "] Started. Publishes to: execution.requests, notifications");
    }

    /**
     * Registers a workflow trigger. In production, this info would come
     * from a shared database or API call, not direct method invocation.
     */
    public void registerTrigger(String workflowId, String workflowName,
                                 String triggerType, String triggerValue) {
        registeredTriggers.put(workflowId,
                new WorkflowTriggerInfo(workflowId, workflowName, triggerType, triggerValue));
        LOGGER.info(() -> "[" + SERVICE_NAME + "] Registered trigger: "
                + triggerType + "(" + triggerValue + ") → " + workflowName);
    }

    /**
     * Simulates a trigger firing — publishes execution request to the bus.
     * In production: cron scheduler fires, webhook endpoint hit, file detected.
     */
    public void fireTrigger(String workflowId) {
        WorkflowTriggerInfo info = registeredTriggers.get(workflowId);
        if (info == null) {
            LOGGER.info(() -> "[" + SERVICE_NAME + "] Unknown workflow ID: " + workflowId);
            return;
        }

        LOGGER.info(() -> "[" + SERVICE_NAME + "] Trigger fired: "
                + info.triggerType + "(" + info.triggerValue + ") → " + info.workflowName);

                // Notify about trigger firing
        bus.publish("notifications", ServiceMessage.of("TRIGGER_FIRED", SERVICE_NAME,
                Map.of(KEY_WORKFLOW_NAME, info.workflowName,
                        "triggerType", info.triggerType,
                        "triggerValue", info.triggerValue)));

        // Request execution
        bus.publish("execution.requests", ServiceMessage.of("EXECUTE", SERVICE_NAME,
                Map.of("workflowId", info.workflowId,
                        KEY_WORKFLOW_NAME, info.workflowName,
                        "triggeredBy", info.triggerType)));

        // Publish to analytics
        bus.publish("analytics", ServiceMessage.of("TRIGGER_FIRED", SERVICE_NAME,
                Map.of(KEY_WORKFLOW_NAME, info.workflowName,
                        "triggerType", info.triggerType)));
    }

    /**
     * Simulates a webhook being received.
     */
    public void simulateWebhook(String path) {
        for (WorkflowTriggerInfo info : registeredTriggers.values()) {
            if ("WEBHOOK".equals(info.triggerType) && info.triggerValue.equals(path)) {
                LOGGER.info(() -> "[" + SERVICE_NAME + "] Webhook matched: " + path);
                fireTrigger(info.workflowId);
                return;
            }
        }
        LOGGER.info(() -> "[" + SERVICE_NAME + "] No workflow registered for webhook: " + path);
    }

    private record WorkflowTriggerInfo(String workflowId, String workflowName,
                                        String triggerType, String triggerValue) {
    }
}
