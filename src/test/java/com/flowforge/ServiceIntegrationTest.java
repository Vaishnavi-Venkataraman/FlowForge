package com.flowforge;

import com.flowforge.engine.WorkflowBuilder;
import com.flowforge.engine.WorkflowEngine;
import com.flowforge.engine.strategy.ParallelStrategy;
import com.flowforge.event.EventBus;
import com.flowforge.model.WorkflowDefinition;
import com.flowforge.service.*;
import com.flowforge.task.TaskFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ServiceIntegrationTest {

    private ServiceBus bus;
    private WorkflowEngine engine;
    private TaskFactory factory;

    @BeforeEach
    void setUp() {
        bus = new ServiceBus();
        factory = new TaskFactory();
        EventBus eventBus = new EventBus();
        engine = new WorkflowEngine(factory, eventBus);
        engine.registerStrategy(new ParallelStrategy());
    }

    @Test
    void executionServiceShouldHandleRequest() {
        new ExecutionService(engine, bus);
        WorkflowDefinition wf = WorkflowBuilder.create()
                .name("Exec Test").manualTrigger().addDelayTask("w", 0).build();
        String id = engine.registerWorkflow(wf);

        assertDoesNotThrow(() ->
                bus.publish("execution.requests", ServiceMessage.of("EXECUTE", "test",
                        Map.of("workflowId", id, "workflowName", "Exec Test"))));
    }

    @Test
    void executionServiceShouldHandleMissingId() {
        new ExecutionService(engine, bus);
        assertDoesNotThrow(() ->
                bus.publish("execution.requests", ServiceMessage.of("EXECUTE", "test",
                        Map.of("workflowName", "No ID"))));
    }

    @Test
    void executionServiceShouldHandleFailedWorkflow() {
        new ExecutionService(engine, bus);
        assertDoesNotThrow(() ->
                bus.publish("execution.requests", ServiceMessage.of("EXECUTE", "test",
                        Map.of("workflowId", "nonexistent", "workflowName", "Bad"))));
    }

    @Test
    void triggerServiceShouldFireAndExecute() {
        new ExecutionService(engine, bus);
        new NotificationService(bus);
        new AnalyticsService(bus);
        new AuditService(bus);
        TriggerService triggerService = new TriggerService(bus);

        WorkflowDefinition wf = WorkflowBuilder.create()
                .name("Trigger Test").manualTrigger().addDelayTask("w", 0).build();
        String id = engine.registerWorkflow(wf);
        triggerService.registerTrigger(id, "Trigger Test", "MANUAL", "");

        assertDoesNotThrow(() -> triggerService.fireTrigger(id));
    }

    @Test
    void triggerServiceShouldHandleUnknownId() {
        TriggerService triggerService = new TriggerService(bus);
        assertDoesNotThrow(() -> triggerService.fireTrigger("nonexistent-id"));
    }

    @Test
    void triggerServiceShouldSimulateWebhook() {
        new ExecutionService(engine, bus);
        TriggerService triggerService = new TriggerService(bus);

        WorkflowDefinition wf = WorkflowBuilder.create()
                .name("Webhook WF").webhookTrigger("/api/hook").addDelayTask("w", 0).build();
        String id = engine.registerWorkflow(wf);
        triggerService.registerTrigger(id, "Webhook WF", "WEBHOOK", "/api/hook");

        assertDoesNotThrow(() -> triggerService.simulateWebhook("/api/hook"));
    }

    @Test
    void triggerServiceShouldHandleUnmatchedWebhook() {
        TriggerService triggerService = new TriggerService(bus);
        assertDoesNotThrow(() -> triggerService.simulateWebhook("/unknown"));
    }

    @Test
    void notificationServiceShouldHandleAllEventTypes() {
        new NotificationService(bus);

        assertDoesNotThrow(() -> {
            bus.publish("notifications", ServiceMessage.of("WORKFLOW_COMPLETED", "test",
                    Map.of("workflowName", "WF1")));
            bus.publish("notifications", ServiceMessage.of("WORKFLOW_FAILED", "test",
                    Map.of("workflowName", "WF2", "error", "fail")));
            bus.publish("notifications", ServiceMessage.of("TRIGGER_FIRED", "test",
                    Map.of("workflowName", "WF3", "triggerType", "CRON")));
        });
    }

    @Test
    void analyticsServiceShouldTrackMetrics() {
        AnalyticsService analytics = new AnalyticsService(bus);

        bus.publish("analytics", ServiceMessage.of("EXECUTION_COMPLETE", "test",
                Map.of("workflowName", "WF1")));
        bus.publish("analytics", ServiceMessage.of("EXECUTION_FAILED", "test",
                Map.of("workflowName", "WF2", "error", "err")));
        bus.publish("analytics", ServiceMessage.of("TRIGGER_FIRED", "test",
                Map.of("workflowName", "WF3")));

        assertDoesNotThrow(analytics::printDashboard);
    }

    @Test
    void auditServiceShouldLogEvents() {
        new AuditService(bus);
        assertDoesNotThrow(() -> {
            bus.publish("audit", ServiceMessage.of("EXECUTION_SUCCESS", "test",
                    Map.of("workflowName", "WF1")));
            bus.publish("audit", ServiceMessage.of("EXECUTION_FAILURE", "test",
                    Map.of("workflowName", "WF2", "error", "failed")));
        });
    }

    @Test
    void serviceMessageShouldExposeFields() {
        ServiceMessage msg = ServiceMessage.of("ACT", "SRC", Map.of("k", "v"));
        assertEquals("ACT", msg.getAction());
        assertEquals("SRC", msg.getSourceService());
        assertEquals("v", msg.getPayloadValue("k", "default"));
        assertEquals("default", msg.getPayloadValue("missing", "default"));
        assertNotNull(msg.getTimestamp());
        assertNotNull(msg.toString());
    }

    @Test
    void serviceMessageShouldSupportReplyChannel() {
        ServiceMessage msg = ServiceMessage.of("ACT", "SRC");
        assertNull(msg.getReplyChannel());
        msg.setReplyChannel("reply.ch");
        assertEquals("reply.ch", msg.getReplyChannel());
    }
}
