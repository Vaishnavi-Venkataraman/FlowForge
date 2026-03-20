package com.flowforge.web;

import java.util.*;

/**
 * Minimal JSON serializer/deserializer — no external dependencies.
 * Handles the subset we need: objects, arrays, strings, numbers, booleans.
 */
public final class JsonUtil {

    private JsonUtil() {}

    // --- Serialization ---

    public static String toJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(escape(entry.getKey())).append("\":");
            sb.append(valueToJson(entry.getValue()));
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    public static String toJsonArray(List<Map<String, Object>> list) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (Map<String, Object> item : list) {
            if (!first) sb.append(",");
            sb.append(toJson(item));
            first = false;
        }
        sb.append("]");
        return sb.toString();
    }

    private static String valueToJson(Object value) {
        if (value == null) return "null";
        if (value instanceof String s) return "\"" + escape(s) + "\"";
        if (value instanceof Number || value instanceof Boolean) return value.toString();
        if (value instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) value;
            return toJson(map);
        }
        if (value instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> list = (List<Map<String, Object>>) value;
            return toJsonArray(list);
        }
        return "\"" + escape(value.toString()) + "\"";
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    // --- Deserialization (simple key-value pairs from POST body) ---

    public static Map<String, String> parseJsonFlat(String json) {
        // NOSONAR - complexity is inherent to JSON parsing without library
        Map<String, String> result = new LinkedHashMap<>();
        json = json.trim();
        if (json.startsWith("{")) json = json.substring(1);
        if (json.endsWith("}")) json = json.substring(0, json.length() - 1);

        int i = 0;
        while (i < json.length()) {
            i = skipWhitespace(json, i);
            if (i >= json.length()) break;

            // Parse key
            if (json.charAt(i) != '"') { i++; continue; }
            int keyStart = i + 1;
            int keyEnd = json.indexOf('"', keyStart);
            if (keyEnd < 0) break;
            String key = json.substring(keyStart, keyEnd);

            i = keyEnd + 1;
            i = skipWhitespace(json, i);
            if (i >= json.length() || json.charAt(i) != ':') break;
            i++;
            i = skipWhitespace(json, i);

            // Parse value
            String value;
            if (json.charAt(i) == '"') {
                int valStart = i + 1;
                int valEnd = findUnescapedQuote(json, valStart);
                value = json.substring(valStart, valEnd).replace("\\\"", "\"").replace("\\n", "\n");
                i = valEnd + 1;
            } else if (json.charAt(i) == '[') {
                int depth = 1;
                int arrStart = i;
                i++;
                while (i < json.length() && depth > 0) {
                    if (json.charAt(i) == '[') depth++;
                    else if (json.charAt(i) == ']') depth--;
                    i++;
                }
                value = json.substring(arrStart, i);
            } else {
                int valStart = i;
                while (i < json.length() && json.charAt(i) != ',' && json.charAt(i) != '}') i++;
                value = json.substring(valStart, i).trim();
            }

            result.put(key, value);
            if (i < json.length() && json.charAt(i) == ',') i++;
        }
        return result;
    }

    private static int skipWhitespace(String s, int i) {
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        return i;
    }

    private static int findUnescapedQuote(String s, int start) {
        for (int i = start; i < s.length(); i++) {
            if (s.charAt(i) == '"' && (i == 0 || s.charAt(i - 1) != '\\')) return i;
        }
        return s.length();
    }
}