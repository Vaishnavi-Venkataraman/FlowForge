package com.flowforge.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class JsonUtil {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private static final Type MAP_TYPE = new TypeToken<LinkedHashMap<String, Object>>() {}.getType();

    private JsonUtil() {}

    public static String toJson(Map<String, Object> map) {
        return GSON.toJson(map);
    }

    public static String toJsonArray(List<Map<String, Object>> list) {
        return GSON.toJson(list);
    }

    public static Map<String, String> parseJsonFlat(String json) {
        if (json == null || json.isBlank() || !json.trim().startsWith("{")) {
            return new LinkedHashMap<>();
        }

        try {
            Map<String, Object> parsed = GSON.fromJson(json.trim(), MAP_TYPE);
            if (parsed == null) {
                return new LinkedHashMap<>();
            }
            return flattenValues(parsed);
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    private static Map<String, String> flattenValues(Map<String, Object> parsed) {
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : parsed.entrySet()) {
            result.put(entry.getKey(), valueToString(entry.getValue()));
        }
        return result;
    }

    private static String valueToString(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Map || value instanceof List) {
            return GSON.toJson(value);
        }
        if (value instanceof Number num && num.doubleValue() == num.longValue()) {
            return String.valueOf(num.longValue());
        }
        return value.toString();
    }
}