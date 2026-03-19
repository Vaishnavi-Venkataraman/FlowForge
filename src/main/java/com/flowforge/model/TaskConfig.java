package com.flowforge.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Typed configuration for a task.
 *
 * WHY: Replaces the HashMap<String, Object> used in the naive implementation
 * for task config. Provides a name, type, and a typed parameter map with
 * a safe accessor method.
 */
public class TaskConfig {

    private final String name;
    private final String type;
    private final Map<String, String> parameters;

    public TaskConfig(String name, String type, Map<String, String> parameters) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Task name cannot be null or blank");
        }
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Task type cannot be null or blank");
        }
        this.name = name;
        this.type = type;
        this.parameters = parameters != null
                ? Collections.unmodifiableMap(new HashMap<>(parameters))
                : Collections.emptyMap();
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    /**
     * Safely retrieves a parameter value.
     *
     * @param key          the parameter key
     * @param defaultValue returned if key is absent
     * @return the value or defaultValue
     */
    public String getParameter(String key, String defaultValue) {
        return parameters.getOrDefault(key, defaultValue);
    }

    public String getRequiredParameter(String key) {
        String value = parameters.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Missing required parameter: " + key + " in task: " + name);
        }
        return value;
    }

    @Override
    public String toString() {
        return "TaskConfig{name='" + name + "', type='" + type + "', params=" + parameters + "}";
    }
}