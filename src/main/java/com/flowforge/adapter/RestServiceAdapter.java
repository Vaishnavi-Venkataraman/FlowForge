package com.flowforge.adapter;

import com.flowforge.adapter.thirdparty.RestApiClient;

import java.util.Map;

/**
 * Adapter that wraps RestApiClient to conform to ExternalService.
 */
public class RestServiceAdapter implements ExternalService {

    private final String serviceId;
    private final String serviceName;
    private final RestApiClient client;

    public RestServiceAdapter(String serviceId, String serviceName, RestApiClient client) {
        this.serviceId = serviceId;
        this.serviceName = serviceName;
        this.client = client;
    }

    @Override
    public String getServiceId() {
        return serviceId;
    }

    @Override
    public String getServiceName() {
        return serviceName;
    }

    @Override
    public String getProtocol() {
        return "REST";
    }

    @Override
    public boolean healthCheck() {
        return client.ping();
    }

    @Override
    public ServiceResponse execute(String operation, Map<String, String> parameters) {
        String path = parameters.getOrDefault("path", "/");
        String method = operation != null ? operation : "GET";

        try {
            // Adapt: our interface → RestApiClient interface
            RestApiClient.RestResponse restResponse = client.sendRequest(method, path, parameters);

            // Adapt: RestApiClient response → our ServiceResponse
            if (restResponse.httpStatus >= 200 && restResponse.httpStatus < 300) {
                return ServiceResponse.success(
                        restResponse.httpStatus,
                        restResponse.jsonBody,
                        restResponse.responseHeaders
                );
            } else {
                return ServiceResponse.failure(
                        restResponse.httpStatus,
                        "HTTP error: " + restResponse.httpStatus
                );
            }
        } catch (Exception e) {
            return ServiceResponse.failure(500, "REST adapter error: " + e.getMessage());
        }
    }
}