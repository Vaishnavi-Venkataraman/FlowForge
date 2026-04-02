package com.flowforge;

import com.flowforge.engine.WorkflowEngine;
import com.flowforge.event.EventBus;
import com.flowforge.service.*;
import com.flowforge.task.TaskFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ServiceTest {

    private ServiceBus bus;

    @BeforeEach
    void setUp() {
        bus = new ServiceBus();
    }

    @Test
    void shouldPublishAndSubscribe() {
        List<String> received = new ArrayList<>();
        bus.subscribe("test-channel", msg -> received.add(msg.getAction()));
        bus.publish("test-channel", ServiceMessage.of("DO_SOMETHING", "test-source"));
        assertEquals(1, received.size());
        assertEquals("DO_SOMETHING", received.get(0));
    }

    @Test
    void shouldPublishWithPayload() {
        List<String> received = new ArrayList<>();
        bus.subscribe("ch", msg -> received.add(msg.getPayloadValue("key", "none")));
        bus.publish("ch", ServiceMessage.of("ACT", "src", Map.of("key", "value")));
        assertEquals("value", received.get(0));
    }

    @Test
    void shouldNotFailOnUnsubscribedChannel() {
        assertDoesNotThrow(() ->
                bus.publish("empty-channel", ServiceMessage.of("X", "src")));
    }

    @Test
    void shouldCreateNotificationService() {
        NotificationService svc = new NotificationService(bus);
        assertNotNull(svc);
    }

    @Test
    void shouldCreateAnalyticsService() {
        AnalyticsService svc = new AnalyticsService(bus);
        assertNotNull(svc);
    }

    @Test
    void shouldCreateAuditService() {
        AuditService svc = new AuditService(bus);
        assertNotNull(svc);
    }

    @Test
    void shouldCreateTriggerService() {
        TriggerService svc = new TriggerService(bus);
        assertNotNull(svc);
    }

    @Test
    void shouldCreateExecutionService() {
        EventBus eventBus = new EventBus();
        TaskFactory factory = new TaskFactory();
        WorkflowEngine engine = new WorkflowEngine(factory, eventBus);
        ExecutionService svc = new ExecutionService(engine, bus);
        assertNotNull(svc);
    }

    @Test
    void shouldHandleNotificationEvent() {
        new NotificationService(bus);
        assertDoesNotThrow(() ->
                bus.publish("notifications", ServiceMessage.of("WORKFLOW_COMPLETED", "test",
                        Map.of("workflowName", "TestWF"))));
    }

    @Test
    void shouldHandleAnalyticsEvent() {
        new AnalyticsService(bus);
        assertDoesNotThrow(() ->
                bus.publish("analytics", ServiceMessage.of("EXECUTION_COMPLETE", "test",
                        Map.of("workflowName", "TestWF"))));
    }

    @Test
    void shouldHandleAuditEvent() {
        new AuditService(bus);
        assertDoesNotThrow(() ->
                bus.publish("audit", ServiceMessage.of("EXECUTION_COMPLETE", "test",
                        Map.of("workflowName", "TestWF"))));
    }
}