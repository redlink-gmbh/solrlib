/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package io.redlink.solrlib.cloud;

import io.redlink.solrlib.SolrCoreContainer;
import io.redlink.solrlib.SolrCoreDescriptor;
import io.redlink.utils.PathUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * Created by jakob on 25.01.17.
 */
public class SolrCloudConnector extends SolrCoreContainer {

    private final SolrCloudConnectorConfiguration config;

    public SolrCloudConnector(Set<SolrCoreDescriptor> coreDescriptors, SolrCloudConnectorConfiguration configuration) {
        this(coreDescriptors, configuration, null);
    }
    public SolrCloudConnector(Set<SolrCoreDescriptor> coreDescriptors, SolrCloudConnectorConfiguration configuration, ExecutorService executorService) {
        super(coreDescriptors, executorService);

        this.config = configuration;
    }

    @Override
    protected void init() throws IOException {
        final String prefix = StringUtils.isNotBlank(config.getPrefix())?config.getPrefix()+"_":"";
        final Path sharedLibs = Files.createTempDirectory("solrSharedLibs");
        try (CloudSolrClient client = createSolrClient()) {
            for (SolrCoreDescriptor coreDescriptor : coreDescriptors) {
                final String coreName = coreDescriptor.getCoreName();
                final String remoteName = prefix + coreName;
                if (availableCores.contains(coreName)) {
                    log.warn("CoreName-Clash: {} already initialized. Skipping {}", coreName, coreDescriptor.getClass());
                    continue;
                }

                final Path tmp = Files.createTempDirectory(coreName);
                try {
                    coreDescriptor.initCoreDirectory(tmp, sharedLibs);

                    client.uploadConfig(tmp, remoteName);

                    // TODO: Check and log the response
                    client.request(CollectionAdminRequest
                                    .createCollection(remoteName, remoteName,
                                            Math.max(1, coreDescriptor.getNumShards()),
                                            Math.max(2, coreDescriptor.getReplicationFactor())
                                    )
                                    .setMaxShardsPerNode(config.getMaxShardsPerNode())
                    );
                } catch (SolrServerException e) {
                    log.debug("Initializing core {} ({}) failed: {}", coreName, remoteName, e.getMessage());
                    throw new IOException(String.format("Initializing collection %s (%s) failed", coreName, remoteName), e);
                } finally {
                    PathUtils.deleteRecursive(tmp);
                }
                availableCores.add(coreName);
            }
        } finally {
            PathUtils.deleteRecursive(sharedLibs);
        }
    }

    private CloudSolrClient createSolrClient() {
        return new CloudSolrClient.Builder()
                .withZkHost(config.getZkConnection())
                .build();
    }

    @Override
    protected SolrClient createSolrClient(String coreName) {
        return createSolrClient();
    }
}
