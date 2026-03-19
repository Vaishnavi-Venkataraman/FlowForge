package com.flowforge.model;

/**
 * Configuration for a workflow trigger.
 *
 * WHY: Replaces the raw triggerType/triggerValue strings stored in the
 * HashMap workflow definition. Provides type safety through TriggerType enum.
 */
public class TriggerConfig {

    private final TriggerType type;
    private final String value;

    public TriggerConfig(TriggerType type, String value) {
        if (type == null) {
            throw new IllegalArgumentException("Trigger type cannot be null");
        }
        this.type = type;
        this.value = value;
    }

    public TriggerType getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "TriggerConfig{type=" + type + ", value='" + value + "'}";
    }
}