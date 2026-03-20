package com.flowforge.adapter;
import java.util.logging.Logger;
import com.flowforge.model.TaskConfig;
import com.flowforge.task.AbstractTask;

import java.util.HashMap;
import java.util.Map;

public class ExternalServiceTask extends AbstractTask {
    private static final Logger LOGGER = Logger.getLogger(ExternalServiceTask.class.getName());
    private static final String KEY_SERVICE_ID = "serviceId";
    private static final String KEY_OPERATION = "operation";
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
        String serviceId = config.getRequiredParameter(KEY_SERVICE_ID);
        config.getRequiredParameter(KEY_OPERATION);

        if (!serviceRegistry.hasService(serviceId)) {
            throw new IllegalArgumentException("Service not found: " + serviceId);
        }
    }

    @Override
    protected String doExecute(TaskConfig config) throws Exception { // NOSONAR - broad catch needed for plugin extensibility
        String serviceId = config.getRequiredParameter(KEY_SERVICE_ID);
        String operation = config.getRequiredParameter(KEY_OPERATION);

        // Collect all params EXCEPT serviceId and operation
        Map<String, String> serviceParams = new HashMap<>(config.getParameters());
        serviceParams.remove(KEY_SERVICE_ID);
        serviceParams.remove(KEY_OPERATION);

        ExternalService service = serviceRegistry.getService(serviceId);

        LOGGER.info(() -> "    Calling " + service.getServiceName()
                + " [" + service.getProtocol() + "] — operation: " + operation);

        ServiceResponse response = service.execute(operation, serviceParams);

        if (response.isSuccess()) {
            return service.getServiceName() + " → " + response.getBody();
        } else {
            throw new Exception(service.getServiceName() + " failed: " + response.getErrorMessage());
        }
    }
}
