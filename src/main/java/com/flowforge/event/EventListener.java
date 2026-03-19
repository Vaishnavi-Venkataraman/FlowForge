package com.flowforge.event;

/**
 * Observer interface for workflow events.
 */
public interface EventListener {

    /**
     * Called when an event is published to the bus.
     *
     * @param event the workflow event
     */
    void onEvent(WorkflowEvent event);
}