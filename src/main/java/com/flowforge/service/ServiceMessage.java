package com.flowforge.service;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/* Message envelope for inter-service communication. */
public class ServiceMessage {

    private final String action;
    private final String sourceService;
    private final Map<String, String> payload;
    private final Instant timestamp;
    private String replyChannel;

    public ServiceMessage(String action, String sourceService, Map<String, String> payload) {
        this.action = action;
        this.sourceService = sourceService;
        this.payload = payload != null ? new HashMap<>(payload) : new HashMap<>();
        this.timestamp = Instant.now();
    }

    public static ServiceMessage of(String action, String source) {
        return new ServiceMessage(action, source, null);
    }

    public static ServiceMessage of(String action, String source, Map<String, String> payload) {
        return new ServiceMessage(action, source, payload);
    }

    public String getAction() {
        return action;
    }

    public String getSourceService() {
        return sourceService;
    }

    public Map<String, String> getPayload() {
        return Collections.unmodifiableMap(payload);
    }

    public String getPayloadValue(String key, String defaultValue) {
        return payload.getOrDefault(key, defaultValue);
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getReplyChannel() {
        return replyChannel;
    }

    public void setReplyChannel(String replyChannel) {
        this.replyChannel = replyChannel;
    }

    @Override
    public String toString() {
        return "ServiceMessage{action='" + action + "', from='" + sourceService
                + "', payload=" + payload + "}";
    }
}