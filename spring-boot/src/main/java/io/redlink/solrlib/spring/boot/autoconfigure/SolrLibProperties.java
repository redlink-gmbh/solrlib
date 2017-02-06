/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package io.redlink.solrlib.spring.boot.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

/**
 */
@ConfigurationProperties(prefix = "solrlib")
public class SolrLibProperties {

    private Path home = null;

    private String zkConnection = null;
    private int maxShardsPerNode = 1;
    private String collectionPrefix = "";

    public Path getHome() {
        return home;
    }

    public void setHome(Path home) {
        this.home = home;
    }

    public String getZkConnection() {
        return zkConnection;
    }

    public void setZkConnection(String zkConnection) {
        this.zkConnection = zkConnection;
    }

    public int getMaxShardsPerNode() {
        return maxShardsPerNode;
    }

    public void setMaxShardsPerNode(int maxShardsPerNode) {
        this.maxShardsPerNode = maxShardsPerNode;
    }

    public String getCollectionPrefix() {
        return collectionPrefix;
    }

    public void setCollectionPrefix(String collectionPrefix) {
        this.collectionPrefix = collectionPrefix;
    }
}
