package com.flowforge;

import com.flowforge.adapter.*;
import com.flowforge.adapter.thirdparty.CloudStorageSDK;
import com.flowforge.adapter.thirdparty.RestApiClient;
import com.flowforge.adapter.thirdparty.SoapServiceClient;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AdapterTest {

    @Test
    void restAdapterShouldExecuteGet() {
        RestServiceAdapter adapter = new RestServiceAdapter("rest-1", "REST Test",
                new RestApiClient("https://api.test.com"));
        assertEquals("REST", adapter.getProtocol());
        ServiceResponse response = adapter.execute("GET", Map.of("path", "/users"));
        assertTrue(response.isSuccess());
    }

    @Test
    void soapAdapterShouldExecute() {
        SoapServiceAdapter adapter = new SoapServiceAdapter("soap-1", "SOAP Test",
                new SoapServiceClient("https://soap.test.com/ws?wsdl"));
        assertEquals("SOAP", adapter.getProtocol());
        ServiceResponse response = adapter.execute("GetUsers", Map.of());
        assertTrue(response.isSuccess());
    }

    @Test
    void cloudAdapterShouldExecuteUpload() {
        CloudStorageAdapter adapter = new CloudStorageAdapter("cloud-1", "Cloud Test",
                new CloudStorageSDK("us-east-1", "acct-1"));
        assertEquals("CLOUD_STORAGE", adapter.getProtocol());
        ServiceResponse response = adapter.execute("UPLOAD", Map.of("path", "/data.json"));
        assertTrue(response.isSuccess());
    }

    @Test
    void serviceRegistryShouldRegisterAndFind() {
        ServiceRegistry registry = new ServiceRegistry();
        RestServiceAdapter adapter = new RestServiceAdapter("my-api", "My API",
                new RestApiClient("https://my.api.com"));
        registry.register(adapter);
        assertTrue(registry.hasService("my-api"));
        assertNotNull(registry.getService("my-api"));
    }

    @Test
    void serviceRegistryShouldReturnNullForUnknown() {
        ServiceRegistry registry = new ServiceRegistry();
        assertFalse(registry.hasService("nonexistent"));
    }
}
