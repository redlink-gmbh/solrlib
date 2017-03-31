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
import org.apache.solr.common.util.NamedList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 */
public class SolrCloudConnector extends SolrCoreContainer {

    private final SolrCloudConnectorConfiguration config;
    private final String prefix;

    public SolrCloudConnector(Set<SolrCoreDescriptor> coreDescriptors, SolrCloudConnectorConfiguration configuration) {
        this(coreDescriptors, configuration, null);
    }
    public SolrCloudConnector(Set<SolrCoreDescriptor> coreDescriptors, SolrCloudConnectorConfiguration configuration, ExecutorService executorService) {
        super(coreDescriptors, executorService);

        this.config = configuration;
        this.prefix = StringUtils.defaultString(configuration.getPrefix());
    }

    @Override
    protected void init(ExecutorService executorService) throws IOException, SolrServerException {
        final Path sharedLibs = Files.createTempDirectory("solrSharedLibs");
        try (CloudSolrClient client = createSolrClient()) {
            final NamedList<Object> list = client.request(CollectionAdminRequest.listCollections());
            @SuppressWarnings("unchecked")
            final List<String> existingCollections = (List<String>) list.get("collections");

            for (SolrCoreDescriptor coreDescriptor : coreDescriptors) {
                final String coreName = coreDescriptor.getCoreName();
                final String remoteName = createRemoteName(coreName);
                if (availableCores.containsKey(coreName)) {
                    log.warn("CoreName-Clash: {} already initialized. Skipping {}", coreName, coreDescriptor.getClass());
                    continue;
                } else {
                    log.info("Initializing Core {} (remote: {})", coreName, remoteName);
                }

                if (config.isDeployCores()) {
                    final Path tmp = Files.createTempDirectory(coreName);
                    try {
                        coreDescriptor.initCoreDirectory(tmp, sharedLibs);

                        client.uploadConfig(tmp.resolve("conf"), remoteName);

                        if (!existingCollections.contains(remoteName)) {
                            // TODO: Check and log the response
                            final NamedList<Object> response = client.request(CollectionAdminRequest
                                            .createCollection(remoteName, remoteName,
                                                    Math.max(1, coreDescriptor.getNumShards()),
                                                    Math.max(2, coreDescriptor.getReplicationFactor())
                                            )
                                            .setMaxShardsPerNode(config.getMaxShardsPerNode())
                            );
                            log.debug("Created Collection {}, CoreAdminResponse: {}", coreName, response);
                            scheduleCoreInit(executorService, coreDescriptor, true);
                        } else {
                            log.debug("Collection {} already exists in SolrCloud '{}' as {}", coreName, config.getZkConnection(), remoteName);
                            // TODO: Check and log the response
                            final NamedList<Object> response = client.request(CollectionAdminRequest.reloadCollection(remoteName));
                            log.debug("Reloaded Collection {}, CoreAdminResponse: {}", coreName, response);
                            scheduleCoreInit(executorService, coreDescriptor, false);
                        }
                        availableCores.put(coreName, coreDescriptor);
                    } catch (SolrServerException e) {
                        log.debug("Initializing core {} ({}) failed: {}", coreName, remoteName, e.getMessage());
                        throw new IOException(String.format("Initializing collection %s (%s) failed", coreName, remoteName), e);
                    } finally {
                        PathUtils.deleteRecursive(tmp);
                    }
                } else {
                    if (existingCollections.contains(remoteName)) {
                        log.debug("Collection {} exists in SolrCloud '{}' as {}", coreName, config.getZkConnection(), remoteName);
                        scheduleCoreInit(executorService, coreDescriptor, false);
                        availableCores.put(coreName, coreDescriptor);
                    } else {
                        log.warn("Collection {} (remote: {}) not available in SolrCloud '{}' but deployCores is set to false", coreName, remoteName, config.getZkConnection());
                    }
                }
            }
            log.info("Initialized {} collections in Solr-Cloud {}: {}", availableCores.size(), config.getZkConnection(), availableCores);
        } catch (IOException e) {
            throw e;
        } catch (SolrServerException e) {
            log.error("Could not list existing collections: {}", e.getMessage(), e);
            throw new IOException("Could not list existing collections", e);
        } catch (final Throwable t) {
            log.error("Unexpected {} during init(): {}", t.getClass().getSimpleName(), t.getMessage(), t);
            throw t;
        } finally {
            PathUtils.deleteRecursive(sharedLibs);
        }
    }

    protected String createRemoteName(String coreName) {
        return prefix + coreName;
    }

    protected CloudSolrClient createSolrClient() {
        return new CloudSolrClient.Builder()
                .withZkHost(config.getZkConnection())
                .build();
    }

    @Override
    protected SolrClient createSolrClient(String coreName) {
        CloudSolrClient solrClient = createSolrClient();
        solrClient.setDefaultCollection(createRemoteName(coreName));
        return solrClient;
    }
}
