package com.flowforge.adapter.thirdparty;

import java.util.logging.Logger;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Simulated third-party REST API client.
 */
public class RestApiClient {

    private static final Logger LOGGER = Logger.getLogger(RestApiClient.class.getName());

    private final String baseUrl;

    public RestApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /**
     * Incompatible method signature — returns a custom RestResponse, not ServiceResponse.
     */
    public RestResponse sendRequest(String method, String path, Map<String, String> queryParams) {

        String fullUrl = baseUrl + path;

        // ✅ Use queryParams (fix Sonar warning)
        if (queryParams != null && !queryParams.isEmpty()) {
            StringJoiner joiner = new StringJoiner("&");

            for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                joiner.add(entry.getKey() + "=" + entry.getValue());
            }

            fullUrl += "?" + joiner.toString();
        }

        final String finalUrl = fullUrl;

        LOGGER.info(() -> "      [RestApiClient] " + method + " " + finalUrl);

        // Simulate response
        String responseBody = "{\"status\":\"ok\",\"data\":[{\"id\":1},{\"id\":2}]}";

        return new RestResponse(
                200,
                responseBody,
                Map.of("Content-Type", "application/json")
        );
    }

    public boolean ping() {
        LOGGER.info(() -> "      [RestApiClient] PING " + baseUrl + "/health");
        return true;
    }

    /**
     * Custom response type — incompatible with ServiceResponse.
     */
    public static class RestResponse {

        public final int httpStatus;
        public final String jsonBody;
        public final Map<String, String> responseHeaders;

        public RestResponse(int httpStatus, String jsonBody, Map<String, String> responseHeaders) {
            this.httpStatus = httpStatus;
            this.jsonBody = jsonBody;
            this.responseHeaders = responseHeaders;
        }
    }
}