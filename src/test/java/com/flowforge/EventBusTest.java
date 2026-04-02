package com.flowforge;

import com.flowforge.event.EventBus;
import com.flowforge.event.WorkflowEvent;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Observer pattern — EventBus.
 */
class EventBusTest {

    @Test
    void shouldDeliverEventsToGlobalSubscribers() {
        EventBus bus = new EventBus();
        List<String> received = new ArrayList<>();

        bus.subscribeAll(event -> received.add(event.getType().name()));
        bus.publish(WorkflowEvent.workflowRegistered("TestWF", "id-1"));

        assertEquals(1, received.size());
        assertEquals("WORKFLOW_REGISTERED", received.get(0));
    }

    @Test
    void shouldDeliverToTypedSubscribers() {
        EventBus bus = new EventBus();
        List<String> received = new ArrayList<>();

        bus.subscribe(WorkflowEvent.Type.WORKFLOW_REGISTERED,
                event -> received.add(event.getMessage()));
        bus.publish(WorkflowEvent.workflowRegistered("TestWF", "id-1"));

        assertEquals(1, received.size());
    }

    @Test
    void shouldNotDeliverWrongType() {
        EventBus bus = new EventBus();
        List<String> received = new ArrayList<>();

        bus.subscribe(WorkflowEvent.Type.WORKFLOW_FAILED,
                event -> received.add(event.getMessage()));
        bus.publish(WorkflowEvent.workflowRegistered("TestWF", "id-1"));

        assertTrue(received.isEmpty());
    }

    @Test
    void shouldDeliverToMultipleGlobalSubscribers() {
        EventBus bus = new EventBus();
        List<String> sub1 = new ArrayList<>();
        List<String> sub2 = new ArrayList<>();

        bus.subscribeAll(event -> sub1.add(event.getType().name()));
        bus.subscribeAll(event -> sub2.add(event.getType().name()));
        bus.publish(WorkflowEvent.workflowRegistered("WF", "id-1"));

        assertEquals(1, sub1.size());
        assertEquals(1, sub2.size());
    }

    @Test
    void shouldNotFailWithNoSubscribers() {
        EventBus bus = new EventBus();
        assertDoesNotThrow(() ->
                bus.publish(WorkflowEvent.workflowRegistered("WF", "id-1")));
    }
}