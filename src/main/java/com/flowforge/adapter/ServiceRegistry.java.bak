package com.flowforge.adapter;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class ServiceRegistry {
    private static final Logger LOGGER = Logger.getLogger(ServiceRegistry.class.getName());
    private final Map<String, ExternalService> services = new HashMap<>();

    public void register(ExternalService service) {
        if (service == null) {
            throw new IllegalArgumentException("Service cannot be null");
        }
        services.put(service.getServiceId(), service);
        LOGGER.info("[ServiceRegistry] Registered: " + service.getServiceName()
                + " [" + service.getServiceId() + ", protocol=" + service.getProtocol() + "]");
    }

    public ExternalService getService(String serviceId) {
        ExternalService service = services.get(serviceId);
        if (service == null) {
            throw new IllegalArgumentException("Unknown service: " + serviceId
                    + ". Registered: " + services.keySet());
        }
        return service;
    }

    public boolean hasService(String serviceId) {
        return services.containsKey(serviceId);
    }

    public Collection<ExternalService> getAllServices() {
        return Collections.unmodifiableCollection(services.values());
    }

    /**
     * Health-checks all registered services.
     */
    public void healthCheckAll() {
        LOGGER.info("=== Service Health Check ===");
        for (ExternalService service : services.values()) {
            boolean healthy = false;
            try {
                healthy = service.healthCheck();
            } catch (Exception e) {
                // health check failed
            }
            String status = healthy ? "HEALTHY" : "UNHEALTHY";
            LOGGER.info("  " + service.getServiceName()
                    + " [" + service.getProtocol() + "] — " + status);
        }
    }
}