package com.flowforge.adapter;

import java.util.Map;

/**
 * Adapters are the translators between incompatible APIs and this interface.
 */
public interface ExternalService {

    /**
     * @return unique identifier for this service
     */
    String getServiceId();

    /**
     * @return human-readable name
     */
    String getServiceName();

    /**
     * @return the protocol/type (REST, SOAP, GRPC, S3, KAFKA, etc.)
     */
    String getProtocol();

    /**
     * Tests whether the service is reachable.
     *
     * @return true if the service is healthy
     */
    boolean healthCheck();

    /**
     * Executes a request against the external service.
     *
     * @param operation  the operation to perform (e.g., "GET", "PUT", "PUBLISH")
     * @param parameters operation-specific parameters
     * @return response from the service
     */
    ServiceResponse execute(String operation, Map<String, String> parameters);
}