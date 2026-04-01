package com.flowforge.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON utility powered by Gson.
 *
 * Provides the same public API as the original hand-written parser,
 * but delegates to Google Gson for robust, standards-compliant JSON handling.
 * This eliminates the cognitive complexity of manual parsing.
 */
public final class JsonUtil {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private static final Type MAP_TYPE = new TypeToken<LinkedHashMap<String, Object>>() {}.getType();

    private JsonUtil() {}

    /**
     * Serializes a Map to a JSON object string.
     * Used by HttpHelper.sendJson() to build API responses.
     *
     * @param map key-value pairs to serialize
     * @return JSON string, e.g. {"id":"abc","name":"My Workflow"}
     */
    public static String toJson(Map<String, Object> map) {
        return GSON.toJson(map);
    }

    /**
     * Serializes a list of Maps to a JSON array string.
     * Used by WorkflowApiHandler.listWorkflows() to return workflow lists.
     *
     * @param list list of maps to serialize
     * @return JSON array string
     */
    public static String toJsonArray(List<Map<String, Object>> list) {
        return GSON.toJson(list);
    }

    /**
     * Parses a JSON object string into a flat Map of String key-value pairs.
     * Used by HttpHelper.parseBody() and WorkflowApiHandler.addParsedTask().
     *
     * Handles nested objects by keeping them as raw JSON strings in the map,
     * preserving backward compatibility with the original hand-written parser.
     *
     * @param json JSON object string
     * @return flat map of string key-value pairs
     */
    public static Map<String, String> parseJsonFlat(String json) {
        Map<String, String> result = new LinkedHashMap<>();

        if (json == null || json.isBlank()) {
            return result;
        }

        json = json.trim();
        if (!json.startsWith("{")) {
            return result;
        }

        try {
            Map<String, Object> parsed = GSON.fromJson(json, MAP_TYPE);
            if (parsed == null) {
                return result;
            }

            for (Map.Entry<String, Object> entry : parsed.entrySet()) {
                Object value = entry.getValue();
                if (value == null) {
                    result.put(entry.getKey(), "null");
                } else if (value instanceof Map || value instanceof List) {
                    // Nested objects/arrays: keep as raw JSON string
                    result.put(entry.getKey(), GSON.toJson(value));
                } else if (value instanceof Number num) {
                    // Gson parses all numbers as Double — convert to clean string
                    if (num.doubleValue() == num.longValue()) {
                        result.put(entry.getKey(), String.valueOf(num.longValue()));
                    } else {
                        result.put(entry.getKey(), num.toString());
                    }
                } else {
                    result.put(entry.getKey(), value.toString());
                }
            }
        } catch (Exception e) {
            // Malformed JSON — return empty map (same behavior as original parser)
            return new LinkedHashMap<>();
        }

        return result;
    }
}