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
package io.redlink.solrlib.embedded;

import io.redlink.solrlib.SolrCoreContainer;

import java.nio.file.Path;

/**
 * Configuration for an {@link EmbeddedCoreContainer}
 */
public class EmbeddedCoreContainerConfiguration {

    /**
     * The solr-home directory. All {@link io.redlink.solrlib.SolrCoreDescriptor}s
     * will be deployed here.
     * If {@code null}, a temp-directory will be created.
     */
    private Path home;

    /**
     * Whether or not to delete {@link #home} on {@link SolrCoreContainer#shutdown()}.
     */
    private boolean deleteOnShutdown = false;

    /**
     * @return the solr-home directory
     * @see #home
     */
    public Path getHome() {
        return home;
    }

    /**
     * @param home the solr-home directory
     * @see  #home
     */
    public void setHome(Path home) {
        this.home = home;
    }

    /**
     * @return flag to indicate if {@link #home} is deleted on {@link SolrCoreContainer#shutdown()}.
     * @see #deleteOnShutdown
     */
    public boolean isDeleteOnShutdown() {
        return deleteOnShutdown;
    }

    /**
     * @param deleteOnShutdown delete {@link #home} on {@link SolrCoreContainer#shutdown()}?
     * @see #deleteOnShutdown
     */
    public void setDeleteOnShutdown(boolean deleteOnShutdown) {
        this.deleteOnShutdown = deleteOnShutdown;
    }
}
