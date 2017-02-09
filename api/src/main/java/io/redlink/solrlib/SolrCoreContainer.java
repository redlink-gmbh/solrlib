/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package io.redlink.solrlib;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 */
public abstract class SolrCoreContainer {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final Optional<ExecutorService> executorService;
    protected final CountDownLatch startupComplete;
    private final AtomicBoolean initStarted;
    private Throwable initException = null;
    protected final Set<SolrCoreDescriptor> coreDescriptors;
    protected final Set<String> availableCores = new HashSet<>();

    public SolrCoreContainer(Set<SolrCoreDescriptor> coreDescriptors, ExecutorService executorService) {
        this.executorService = Optional.ofNullable(executorService);
        startupComplete = new CountDownLatch(1);
        initStarted = new AtomicBoolean(false);
        this.coreDescriptors = coreDescriptors;
    }

    public final void initialize() {
        if (initStarted.compareAndSet(false, true)) {
            final long initStart = System.currentTimeMillis();
            log.debug("Initializing SolrCoreContainer");
            executorService.orElseGet(Executors::newSingleThreadExecutor)
                    .execute(() -> {
                        try {
                            init();
                        } catch (IOException e) {
                            if (log.isDebugEnabled()) {
                                log.error("Error while initializing SolrCoreContainer: {}", e.getMessage(), e);
                            } else {
                                log.error("Error while initializing SolrCoreContainer: {}", e.getMessage());
                            }
                            initException = e;
                        } catch (final Throwable t) {
                            if (log.isDebugEnabled()) {
                                log.error("Unexpected Error while initializing SolrCoreContainer: {}", t.getMessage(), t);
                            } else {
                                log.error("Unexpected Error while initializing SolrCoreContainer: {}", t.getMessage());
                            }
                            initException = t;
                        } finally {
                            long initDuration = System.currentTimeMillis() - initStart;
                            startupComplete.countDown();
                            log.debug("SolrCoreContainer initialized in {}ms", initDuration);
                        }
                    });
        } else {
            if (log.isDebugEnabled()) {
                log.warn("SolrCoreContainer was already initialized, ignoring call. You might have a flaw in your implementation flow...");
                log.debug("Double initialization call, not an error!", new Throwable("SolrCoreContainer already initialized"));
            } else {
                log.warn("SolrCoreContainer was already initialized, ignoring call.");
            }
        }
    }

    protected abstract void init() throws IOException;

    public void shutdown() throws IOException {

    }

    protected abstract SolrClient createSolrClient(String coreName);

    public SolrClient getSolrClient(String coreName) throws SolrServerException {
        try {
            awaitInitCompletion();
            if (initException != null) {
                throw new SolrServerException("Exception initializing SolrCoreContainer", initException);
            }
            return createSolrClient(coreName);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Could not retrieve SolrClient '" + coreName + "'", e);
        }
    }

    protected void awaitInitCompletion() throws InterruptedException {
        startupComplete.await();
    }

    public SolrClient getSolrClient(SolrCoreDescriptor coreDescriptor) throws SolrServerException {
        return getSolrClient(coreDescriptor.getCoreName());
    }
}
