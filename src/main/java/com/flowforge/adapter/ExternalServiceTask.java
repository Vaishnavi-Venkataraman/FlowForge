package com.flowforge.adapter;

import com.flowforge.model.TaskConfig;
import com.flowforge.task.AbstractTask;

import java.util.HashMap;
import java.util.Map;

/**
 * A generic task that delegates to an adapted ExternalService.
 */
public class ExternalServiceTask extends AbstractTask {

    private final ServiceRegistry serviceRegistry;

    public ExternalServiceTask(String name, ServiceRegistry serviceRegistry) {
        super(name);
        this.serviceRegistry = serviceRegistry;
    }

    @Override
    public String getType() {
        return "external_service";
    }

    @Override
    protected void validate(TaskConfig config) {
        String serviceId = config.getRequiredParameter("serviceId");
        config.getRequiredParameter("operation");
        if (!serviceRegistry.hasService(serviceId)) {
            throw new IllegalArgumentException("Service not found: " + serviceId);
        }
    }

    @Override
    protected String doExecute(TaskConfig config) throws Exception {
        String serviceId = config.getRequiredParameter("serviceId");
        String operation = config.getRequiredParameter("operation");

        // Collect all params EXCEPT serviceId and operation — pass the rest to the adapter
        Map<String, String> serviceParams = new HashMap<>(config.getParameters());
        serviceParams.remove("serviceId");
        serviceParams.remove("operation");

        ExternalService service = serviceRegistry.getService(serviceId);
        System.out.println("    Calling " + service.getServiceName()
                + " [" + service.getProtocol() + "] — operation: " + operation);

        ServiceResponse response = service.execute(operation, serviceParams);

        if (response.isSuccess()) {
            return service.getServiceName() + " → " + response.getBody();
        } else {
            throw new Exception(service.getServiceName() + " failed: " + response.getErrorMessage());
        }
    }
}