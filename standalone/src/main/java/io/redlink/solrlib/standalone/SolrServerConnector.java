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
package io.redlink.solrlib.standalone;

import com.google.common.base.Preconditions;
import io.redlink.solrlib.SolrCoreContainer;
import io.redlink.solrlib.SolrCoreDescriptor;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.client.solrj.response.CoreAdminResponse;
import org.apache.solr.common.util.NamedList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * SolrServerConnector, a implementation of {@link SolrCoreContainer} using a standalone Solr-Server as backend.
 * Direct access to {@code $SOLR_HOME} as well as access to the CoreAdminHandler is required.
 */
public class SolrServerConnector extends SolrCoreContainer {

    private final SolrServerConnectorConfiguration configuration;
    private final String prefix;
    private final String solrBaseUrl;
    private final AtomicBoolean initialized;

    public SolrServerConnector(Set<SolrCoreDescriptor> coreDescriptors, SolrServerConnectorConfiguration configuration) {
        this(coreDescriptors, configuration, null);
    }

    public SolrServerConnector(Set<SolrCoreDescriptor> coreDescriptors, SolrServerConnectorConfiguration configuration,
                               ExecutorService executorService) {
        super(coreDescriptors, executorService);
        this.configuration = configuration;
        prefix = StringUtils.defaultString(configuration.getPrefix());
        solrBaseUrl = StringUtils.removeEnd(configuration.getSolrUrl(), "/");
        initialized = new AtomicBoolean(false);
    }

    @Override
    @SuppressWarnings("squid:S3776")
    protected void init(ExecutorService executorService) throws IOException, SolrServerException {
        Preconditions.checkState(initialized.compareAndSet(false, true));
        Preconditions.checkArgument(Objects.nonNull(solrBaseUrl));

        if (configuration.isDeployCores() && Objects.nonNull(configuration.getSolrHome())) {
            final Path solrHome = configuration.getSolrHome();
            Files.createDirectories(solrHome);
            final Path libDir = solrHome.resolve("lib");
            Files.createDirectories(libDir);

            try (HttpSolrClient solrClient = new HttpSolrClient.Builder(solrBaseUrl).build()) {
                for (SolrCoreDescriptor coreDescriptor : coreDescriptors) {
                    final String coreName = coreDescriptor.getCoreName();
                    if (availableCores.containsKey(coreName)) {
                        log.warn("CoreName-Clash: {} already initialized. Skipping {}", coreName, coreDescriptor.getClass());
                        continue;
                    }
                    final String remoteName = createRemoteName(coreName);

                    final Path coreHome = solrHome.resolve(remoteName);
                    coreDescriptor.initCoreDirectory(coreHome, libDir);

                    final Path corePropertiesFile = coreHome.resolve("core.properties");
                    // core.properties is created by the CreateCore-Command.
                    Files.deleteIfExists(corePropertiesFile);

                    if (coreDescriptor.getNumShards() > 1 || coreDescriptor.getReplicationFactor() > 1) {
                        log.warn("Deploying {} to SolrServerConnector, ignoring config of shards={},replication={}", coreName,
                                coreDescriptor.getNumShards(), coreDescriptor.getReplicationFactor());
                    }

                    // Create or reload the core
                    if (CoreAdminRequest.getStatus(remoteName, solrClient).getStartTime(remoteName) == null) {
                        final CoreAdminResponse adminResponse = CoreAdminRequest
                                .createCore(remoteName, coreHome.toAbsolutePath().toString(), solrClient);
                    } else {
                        final CoreAdminResponse adminResponse = CoreAdminRequest
                                .reloadCore(remoteName, solrClient);
                    }
                    // schedule client-side core init
                    final boolean isNewCore = findInNamedList(CoreAdminRequest.getStatus(remoteName, solrClient).getCoreStatus(remoteName),
                            "index", "lastModified") == null;
                    scheduleCoreInit(executorService, coreDescriptor, isNewCore);

                    availableCores.put(coreName, coreDescriptor);
                }
            }
        } else {
            try (HttpSolrClient solrClient = new HttpSolrClient.Builder(solrBaseUrl).build()) {
                for (SolrCoreDescriptor coreDescriptor : coreDescriptors) {
                    final String coreName = coreDescriptor.getCoreName();
                    if (availableCores.containsKey(coreName)) {
                        log.warn("CoreName-Clash: {} already initialized. Skipping {}", coreName, coreDescriptor.getClass());
                        continue;
                    }
                    final String remoteName = createRemoteName(coreName);
                    if (CoreAdminRequest.getStatus(remoteName, solrClient).getStartTime(remoteName) == null) {
                        // Core does not exists
                        log.warn("Collection {} (remote: {}) not available in Solr '{}' " +
                                        "but deployCores is set to false",
                                coreName, remoteName, solrBaseUrl);
                    } else {
                        log.debug("Collection {} exists in Solr '{}' as {}", coreName, solrBaseUrl, remoteName);
                        scheduleCoreInit(executorService, coreDescriptor, false);
                        availableCores.put(coreName, coreDescriptor);
                    }
                }
            }
        }
    }

    private Object findInNamedList(NamedList namedList, String... path) {
        if (path.length < 1) return null;
        final Object value = namedList.get(path[0]);
        if (path.length == 1) return value;

        final NamedList nested = value instanceof NamedList ? ((NamedList) value) : null;
        if (nested != null) return findInNamedList(nested, Arrays.copyOfRange(path, 1, path.length));
        return null;
    }

    protected String createRemoteName(String coreName) {
        return prefix + coreName;
    }


    @Override
    protected SolrClient createSolrClient(String coreName) {
        return new HttpSolrClient.Builder(solrBaseUrl + StringUtils.prependIfMissing(createRemoteName(coreName), "/"))
                .build();
    }
}
