package com.flowforge.event;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventBus {

    private final Map<WorkflowEvent.Type, List<EventListener>> typedListeners;
    private final List<EventListener> globalListeners;

    public EventBus() {
        this.typedListeners = new EnumMap<>(WorkflowEvent.Type.class);
        this.globalListeners = new CopyOnWriteArrayList<>();

        for (WorkflowEvent.Type type : WorkflowEvent.Type.values()) {
            typedListeners.put(type, new CopyOnWriteArrayList<>());
        }
    }

    /**
     * Subscribe to a specific event type.
     */
    public void subscribe(WorkflowEvent.Type eventType, EventListener listener) {
        typedListeners.get(eventType).add(listener);
    }

    /**
     * Subscribe to ALL event types.
     */
    public void subscribeAll(EventListener listener) {
        globalListeners.add(listener);
    }

    /**
     * Publish an event to all matching subscribers.
     */
    public void publish(WorkflowEvent event) {
        List<EventListener> typed = typedListeners.get(event.getType());
        if (typed != null) {
            for (EventListener listener : typed) {
                listener.onEvent(event);
            }
        }

        for (EventListener listener : globalListeners) {
            listener.onEvent(event);
        }
    }
}