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
package io.redlink.solrlib.spring.boot.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

/**
 */
@ConfigurationProperties(prefix = "solrlib")
public class SolrLibProperties {

    private Path home = null;

    private String zkConnection = null;
    private String baseUrl = null;
    private int maxShardsPerNode = 1;
    private String collectionPrefix = "";
    private boolean deployCores = true;
    private boolean deleteOnShutdown = false;

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

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public boolean isDeployCores() {
        return deployCores;
    }

    public void setDeployCores(boolean deployCores) {
        this.deployCores = deployCores;
    }

    public boolean isDeleteOnShutdown() {
        return deleteOnShutdown;
    }

    public void setDeleteOnShutdown(boolean deleteOnShutdown) {
        this.deleteOnShutdown = deleteOnShutdown;
    }
}
