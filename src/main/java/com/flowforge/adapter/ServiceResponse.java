package com.flowforge.adapter;

import java.util.Collections;
import java.util.Map;

/*
 * ServiceResponse normalizes all of these into a common shape that tasks
 * and the engine can process uniformly.
 */
public class ServiceResponse {

    private final boolean success;
    private final int statusCode;
    private final String body;
    private final Map<String, String> headers;
    private final String errorMessage;

    private ServiceResponse(boolean success, int statusCode, String body,
                             Map<String, String> headers, String errorMessage) {
        this.success = success;
        this.statusCode = statusCode;
        this.body = body;
        this.headers = headers != null ? Collections.unmodifiableMap(headers) : Collections.emptyMap();
        this.errorMessage = errorMessage;
    }

    public static ServiceResponse success(int statusCode, String body) {
        return new ServiceResponse(true, statusCode, body, null, null);
    }

    public static ServiceResponse success(int statusCode, String body, Map<String, String> headers) {
        return new ServiceResponse(true, statusCode, body, headers, null);
    }

    public static ServiceResponse failure(int statusCode, String errorMessage) {
        return new ServiceResponse(false, statusCode, null, null, errorMessage);
    }

    public boolean isSuccess() {
        return success;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getBody() {
        return body;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public String toString() {
        if (success) {
            return "ServiceResponse{OK, status=" + statusCode + ", body='" + body + "'}";
        }
        return "ServiceResponse{FAIL, status=" + statusCode + ", error='" + errorMessage + "'}";
    }
}