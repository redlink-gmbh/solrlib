/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package io.redlink.solrlib;

import org.apache.solr.client.solrj.SolrClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 */
public abstract class SolrCoreContainer {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final Optional<ExecutorService> executorService;
    protected final CountDownLatch startupComplete;
    protected final Set<SolrCoreDescriptor> coreDescriptors;
    protected final Set<String> availableCores = new HashSet<>();

    public SolrCoreContainer(Set<SolrCoreDescriptor> coreDescriptors, ExecutorService executorService) {
        this.executorService = Optional.ofNullable(executorService);
        startupComplete = new CountDownLatch(1);
        this.coreDescriptors = coreDescriptors;
    }

    @PostConstruct
    public final void initialize() {
        final long initStart = System.currentTimeMillis();
        executorService.orElseGet(Executors::newSingleThreadExecutor)
                .execute(() -> {
                    try {
                        init();
                    } catch (final Exception t) {
                        if (log.isDebugEnabled()) {
                            log.error("Error while initializing SolrCoreContainer: {}", t.getMessage(), t);
                        } else {
                            log.error("Error while initializing SolrCoreContainer: {}", t.getMessage());
                        }
                    } finally {
                        long initDuration = System.currentTimeMillis() - initStart;
                        startupComplete.countDown();
                        log.debug("SolrCoreContainer initialized in {}ms", initDuration);
                    }
                });
    }

    protected abstract void init() throws IOException;

    @PreDestroy
    public void shutdown() {

    }

    protected abstract SolrClient createSolrClient(String coreName);

    public SolrClient getSolrClient(String coreName) {
        try {
            awaitInitCompletion();
            return createSolrClient(coreName);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Could not retrieve SolrClient '" + coreName + "'", e);
        }
    }

    protected void awaitInitCompletion() throws InterruptedException {
        startupComplete.await();
    }

    public SolrClient getSolrClient(SolrCoreDescriptor coreDescriptor) {
        return getSolrClient(coreDescriptor.getCoreName());
    }
}
