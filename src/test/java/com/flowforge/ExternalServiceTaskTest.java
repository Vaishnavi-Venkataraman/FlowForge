package com.flowforge;

import com.flowforge.adapter.*;
import com.flowforge.adapter.thirdparty.CloudStorageSDK;
import com.flowforge.adapter.thirdparty.RestApiClient;
import com.flowforge.adapter.thirdparty.SoapServiceClient;
import com.flowforge.model.TaskConfig;
import com.flowforge.model.TaskResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExternalServiceTaskTest {

    private ServiceRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new ServiceRegistry();
        registry.register(new RestServiceAdapter("crm", "CRM API", new RestApiClient("https://crm.com")));
        registry.register(new SoapServiceAdapter("erp", "ERP SOAP", new SoapServiceClient("https://erp.com/ws")));
        registry.register(new CloudStorageAdapter("s3", "S3 Store", new CloudStorageSDK("us-east-1", "acct")));
    }

    @Test
    void shouldExecuteRestService() {
        ExternalServiceTask task = new ExternalServiceTask("rest-task", registry);
        TaskResult result = task.execute(new TaskConfig("rest-task", "external_service",
                Map.of("serviceId", "crm", "operation", "GET", "path", "/users")));
        assertTrue(result.isSuccess());
    }

    @Test
    void shouldExecuteSoapService() {
        ExternalServiceTask task = new ExternalServiceTask("soap-task", registry);
        TaskResult result = task.execute(new TaskConfig("soap-task", "external_service",
                Map.of("serviceId", "erp", "operation", "GetInvoices")));
        assertTrue(result.isSuccess());
    }

    @Test
    void shouldExecuteCloudService() {
        ExternalServiceTask task = new ExternalServiceTask("cloud-task", registry);
        TaskResult result = task.execute(new TaskConfig("cloud-task", "external_service",
                Map.of("serviceId", "s3", "operation", "UPLOAD", "path", "/data.json")));
        assertTrue(result.isSuccess());
    }

    @Test
    void shouldFailOnUnknownService() {
        ExternalServiceTask task = new ExternalServiceTask("bad-task", registry);
        TaskResult result = task.execute(new TaskConfig("bad-task", "external_service",
                Map.of("serviceId", "nonexistent", "operation", "GET")));
        assertFalse(result.isSuccess());
    }

    @Test
    void shouldFailWithoutServiceId() {
        ExternalServiceTask task = new ExternalServiceTask("bad-task", registry);
        TaskResult result = task.execute(new TaskConfig("bad-task", "external_service", Map.of()));
        assertFalse(result.isSuccess());
    }

    @Test
    void shouldReturnCorrectType() {
        ExternalServiceTask task = new ExternalServiceTask("t", registry);
        assertEquals("external_service", task.getType());
    }

    @Test
    void registryShouldHealthCheck() {
        assertDoesNotThrow(() -> registry.healthCheckAll());
    }
}
