package com.flowforge.event;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Central event bus implementing the Publish-Subscribe architectural pattern.
 */
public class EventBus {

    private final Map<WorkflowEvent.Type, List<EventListener>> typedListeners;
    private final List<EventListener> globalListeners;

    public EventBus() {
        this.typedListeners = new EnumMap<>(WorkflowEvent.Type.class);
        this.globalListeners = new ArrayList<>();

        for (WorkflowEvent.Type type : WorkflowEvent.Type.values()) {
            typedListeners.put(type, new ArrayList<>());
        }
    }

    /**
     * Subscribe to a specific event type.
     */
    public void subscribe(WorkflowEvent.Type eventType, EventListener listener) {
        typedListeners.get(eventType).add(listener);
    }

    /**
     * Subscribe to ALL event types (wildcard).
     */
    public void subscribeAll(EventListener listener) {
        globalListeners.add(listener);
    }

    /**
     * Publish an event to all matching subscribers.
     */
    public void publish(WorkflowEvent event) {
        // Notify type-specific listeners
        List<EventListener> typed = typedListeners.get(event.getType());
        if (typed != null) {
            for (EventListener listener : typed) {
                listener.onEvent(event);
            }
        }

        // Notify global listeners
        for (EventListener listener : globalListeners) {
            listener.onEvent(event);
        }
    }
}