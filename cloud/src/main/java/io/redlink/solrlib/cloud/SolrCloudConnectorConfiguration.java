/*
 * Copyright 2017 redlink GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
