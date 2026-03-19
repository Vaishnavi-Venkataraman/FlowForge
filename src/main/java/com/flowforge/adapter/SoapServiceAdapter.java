package com.flowforge.adapter;

import com.flowforge.adapter.thirdparty.SoapServiceClient;

import java.util.Map;

/**
 * Adapter that wraps SoapServiceClient to conform to ExternalService.
 */
public class SoapServiceAdapter implements ExternalService {

    private final String serviceId;
    private final String serviceName;
    private final SoapServiceClient client;

    public SoapServiceAdapter(String serviceId, String serviceName, SoapServiceClient client) {
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
        return "SOAP";
    }

    @Override
    public boolean healthCheck() {
        return client.isAvailable();
    }

    @Override
    public ServiceResponse execute(String operation, Map<String, String> parameters) {
        try {
            // Adapt: build XML payload from flat parameters
            String xmlPayload = buildXmlPayload(parameters);

            // Adapt: our interface → SoapServiceClient interface
            SoapServiceClient.SoapEnvelope envelope = client.callOperation(operation, xmlPayload);

            // Adapt: SoapEnvelope → ServiceResponse
            if (envelope.hasFault()) {
                return ServiceResponse.failure(500, "SOAP Fault: " + envelope.faultCode);
            }
            return ServiceResponse.success(200, envelope.xml);

        } catch (Exception e) {
            return ServiceResponse.failure(500, "SOAP adapter error: " + e.getMessage());
        }
    }

    /**
     * Converts flat key-value parameters into a simple XML payload.
     */
    private String buildXmlPayload(Map<String, String> parameters) {
        StringBuilder xml = new StringBuilder("<request>");
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            xml.append("<").append(entry.getKey()).append(">")
               .append(entry.getValue())
               .append("</").append(entry.getKey()).append(">");
        }
        xml.append("</request>");
        return xml.toString();
    }
}