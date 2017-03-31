/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package io.redlink.solrlib.cloud;

/**
 * Configuration-Bean for {@link SolrCloudConnector}.
 */
public class SolrCloudConnectorConfiguration {
    private String zkConnection = "localhost:9983";
    private String prefix = "";
    private int maxShardsPerNode = 1;
    private boolean deployCores = true;

    public String getZkConnection() {
        return zkConnection;
    }

    public void setZkConnection(String zkConnection) {
        this.zkConnection = zkConnection;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public void setMaxShardsPerNode(int maxShardsPerNode) {
        this.maxShardsPerNode = maxShardsPerNode;
    }

    public Integer getMaxShardsPerNode() {
        return maxShardsPerNode;
    }

    public boolean isDeployCores() {
        return deployCores;
    }

    public void setDeployCores(boolean deployCores) {
        this.deployCores = deployCores;
    }
}
