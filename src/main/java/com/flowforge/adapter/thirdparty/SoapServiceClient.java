package com.flowforge.adapter.thirdparty;

/* Simulated third-party legacy SOAP service client. */
public class SoapServiceClient {

    private final String wsdlUrl;

    public SoapServiceClient(String wsdlUrl) {
        this.wsdlUrl = wsdlUrl;
    }

    /**
     * Completely different API — XML envelope in, XML envelope out.
     */
    public SoapEnvelope callOperation(String operationName, String xmlPayload) {
        System.out.println("      [SoapClient] SOAP call: " + operationName + " → " + wsdlUrl);
        System.out.println("      [SoapClient] Payload: " + xmlPayload);

        // Simulate SOAP response
        String responseXml = "<soap:Envelope>"
                + "<soap:Body>"
                + "<" + operationName + "Response>"
                + "<result>SUCCESS</result>"
                + "<recordCount>42</recordCount>"
                + "</" + operationName + "Response>"
                + "</soap:Body>"
                + "</soap:Envelope>";

        return new SoapEnvelope(responseXml, null);
    }

    public boolean isAvailable() {
        System.out.println("      [SoapClient] Checking WSDL: " + wsdlUrl);
        return true;
    }

    /**
     * SOAP-specific response type — XML-based, fault codes.
     */
    public static class SoapEnvelope {
        public final String xml;
        public final String faultCode;

        public SoapEnvelope(String xml, String faultCode) {
            this.xml = xml;
            this.faultCode = faultCode;
        }

        public boolean hasFault() {
            return faultCode != null;
        }
    }
}