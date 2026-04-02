package com.flowforge;

import com.flowforge.web.JsonUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JsonUtilTest {

    @Test
    void shouldSerializeMapToJson() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", "test");
        map.put("count", 42);
        String json = JsonUtil.toJson(map);
        assertTrue(json.contains("\"name\":\"test\""));
        assertTrue(json.contains("\"count\":42"));
    }

    @Test
    void shouldSerializeListToJsonArray() {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", "1");
        String json = JsonUtil.toJsonArray(List.of(item));
        assertTrue(json.startsWith("["));
        assertTrue(json.endsWith("]"));
        assertTrue(json.contains("\"id\":\"1\""));
    }

    @Test
    void shouldParseFlatJson() {
        String json = "{\"name\":\"test\",\"type\":\"http\"}";
        Map<String, String> result = JsonUtil.parseJsonFlat(json);
        assertEquals("test", result.get("name"));
        assertEquals("http", result.get("type"));
    }

    @Test
    void shouldHandleNestedObjectsAsRawJson() {
        String json = "{\"name\":\"task\",\"params\":{\"url\":\"http://x.com\"}}";
        Map<String, String> result = JsonUtil.parseJsonFlat(json);
        assertEquals("task", result.get("name"));
        assertTrue(result.get("params").contains("url"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "not json"})
    void shouldReturnEmptyMapForInvalidInput(String input) {
        Map<String, String> result = JsonUtil.parseJsonFlat(input);
        assertTrue(result.isEmpty());
    }
}