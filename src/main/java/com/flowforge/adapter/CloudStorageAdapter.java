package com.flowforge.adapter;

import com.flowforge.adapter.thirdparty.CloudStorageSDK;

import java.util.Map;

/**
 * Adapter that wraps CloudStorageSDK to conform to ExternalService.
 * Maps our generic parameters to SDK-specific bucket/key addressing.
 */
public class CloudStorageAdapter implements ExternalService {

    private final String serviceId;
    private final String serviceName;
    private final CloudStorageSDK sdk;

    public CloudStorageAdapter(String serviceId, String serviceName, CloudStorageSDK sdk) {
        this.serviceId = serviceId;
        this.serviceName = serviceName;
        this.sdk = sdk;
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
        return "CLOUD_STORAGE";
    }

    @Override
    public boolean healthCheck() {
        String bucket = "health-check-bucket";
        return sdk.bucketExists(bucket);
    }

    @Override
    public ServiceResponse execute(String operation, Map<String, String> parameters) {
        String bucket = parameters.getOrDefault("bucket", "default-bucket");
        String key = parameters.getOrDefault("key", "unnamed");

        try {
            return switch (operation.toUpperCase()) {
                case "UPLOAD" -> {
                    String content = parameters.getOrDefault("content", "");
                    CloudStorageSDK.StorageResult result = sdk.uploadObject(bucket, key, content);
                    yield result.success
                            ? ServiceResponse.success(200, "Uploaded: etag=" + result.etag)
                            : ServiceResponse.failure(500, "Upload failed");
                }
                case "DOWNLOAD" -> {
                    CloudStorageSDK.StorageResult result = sdk.downloadObject(bucket, key);
                    yield result.success
                            ? ServiceResponse.success(200, result.data)
                            : ServiceResponse.failure(404, "Object not found");
                }
                case "DELETE" -> {
                    CloudStorageSDK.StorageResult result = sdk.deleteObject(bucket, key);
                    yield result.success
                            ? ServiceResponse.success(200, "Deleted: " + bucket + "/" + key)
                            : ServiceResponse.failure(500, "Delete failed");
                }
                default -> ServiceResponse.failure(400,
                        "Unsupported storage operation: " + operation);
            };
        } catch (Exception e) {
            return ServiceResponse.failure(500, "Cloud storage adapter error: " + e.getMessage());
        }
    }
}