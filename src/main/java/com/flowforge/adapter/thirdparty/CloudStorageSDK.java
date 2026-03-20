package com.flowforge.adapter.thirdparty;

import java.util.logging.Logger;

/**
 * Simulated cloud storage SDK (like AWS S3, GCP Cloud Storage).
 */
public class CloudStorageSDK {

    private static final Logger LOGGER = Logger.getLogger(CloudStorageSDK.class.getName());

    private static final String ACCOUNT_LABEL = "account="; // ✅ avoids duplication

    private final String region;
    private final String accountId;

    public CloudStorageSDK(String region, String accountId) {
        this.region = region;
        this.accountId = accountId;
    }

    public StorageResult uploadObject(String bucket, String key, String content) {

        final int contentSize = (content != null) ? content.length() : 0;

        LOGGER.info(() -> "      [CloudStorage] UPLOAD s3://" + bucket + "/" + key
                + " (region=" + region
                + ", " + ACCOUNT_LABEL + accountId
                + ", size=" + contentSize + " bytes)");

        return new StorageResult(true, "etag-abc123", bucket + "/" + key);
    }

    public StorageResult downloadObject(String bucket, String key) {
        LOGGER.info(() -> "      [CloudStorage] DOWNLOAD s3://" + bucket + "/" + key
                + " (" + ACCOUNT_LABEL + accountId + ")");
        return new StorageResult(true, null, "file-content-bytes");
    }

    public StorageResult deleteObject(String bucket, String key) {
        LOGGER.info(() -> "      [CloudStorage] DELETE s3://" + bucket + "/" + key
                + " (" + ACCOUNT_LABEL + accountId + ")");
        return new StorageResult(true, null, null);
    }

    public boolean bucketExists(String bucket) {
        LOGGER.info(() -> "      [CloudStorage] HEAD bucket: " + bucket
                + " (" + ACCOUNT_LABEL + accountId + ")");
        return true;
    }

    /**
     * SDK-specific result — etag, object path. Not an HTTP response.
     */
    public static class StorageResult {
        public final boolean success;
        public final String etag;
        public final String data;

        public StorageResult(boolean success, String etag, String data) {
            this.success = success;
            this.etag = etag;
            this.data = data;
        }
    }
}