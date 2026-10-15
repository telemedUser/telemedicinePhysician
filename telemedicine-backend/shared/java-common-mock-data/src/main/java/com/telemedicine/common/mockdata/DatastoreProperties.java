package com.telemedicine.common.mockdata;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Points backend services at the external {@code datastore/seeds/} directory.
 */
@ConfigurationProperties(prefix = "telemedicine.datastore")
public class DatastoreProperties {
    /**
     * Root of seed collections, e.g. {@code file:../datastore/seeds} relative to the backend repo.
     */
    private String root = "file:../datastore/seeds";
    private boolean writable = true;

    public String getRoot() {
        return root;
    }

    public void setRoot(String root) {
        this.root = root;
    }

    public boolean isWritable() {
        return writable;
    }

    public void setWritable(boolean writable) {
        this.writable = writable;
    }

    /** Full Spring resource location for one collection file. */
    public String resolveCollection(String serviceName, String collectionFile) {
        String base = root.endsWith("/") ? root.substring(0, root.length() - 1) : root;
        return base + "/" + serviceName + "/" + collectionFile;
    }
}
