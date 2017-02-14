/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package io.redlink.solrlib;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
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
    private final Map<String, CountDownLatch> coreInitialized;
    private final AtomicBoolean initStarted;
    private Throwable initException = null;
    protected final Map<String, Throwable> coreInitException = new HashMap<>();
    protected final Set<SolrCoreDescriptor> coreDescriptors;
    protected final Map<String, SolrCoreDescriptor> availableCores = new HashMap<>();

    public SolrCoreContainer(Set<SolrCoreDescriptor> coreDescriptors, ExecutorService executorService) {
        this.executorService = Optional.ofNullable(executorService);
        startupComplete = new CountDownLatch(1);
        coreInitialized = new HashMap<>();
        initStarted = new AtomicBoolean(false);
        this.coreDescriptors = coreDescriptors;
    }

    public final void initialize() {
        if (initStarted.compareAndSet(false, true)) {
            final long initStart = System.currentTimeMillis();
            log.debug("Initializing SolrCoreContainer");
            final ExecutorService executorService = this.executorService.orElseGet(Executors::newSingleThreadExecutor);
            executorService
                    .execute(() -> {
                        try {
                            init(executorService);
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
                            if (!this.executorService.isPresent()) {
                                executorService.shutdown();
                            }
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

    protected abstract void init(ExecutorService executorService) throws IOException;

    protected void scheduleCoreInit(ExecutorService executorService, SolrCoreDescriptor coreDescriptor, boolean newCore) {
        final CountDownLatch coreLatch = coreInitialized.computeIfAbsent(coreDescriptor.getCoreName(), s -> new CountDownLatch(1));
        executorService.execute(() -> {
            try {
                awaitInitCompletion();
                try (SolrClient solrClient = createSolrClient(coreDescriptor.getCoreName())) {
                    if (newCore) {
                        coreDescriptor.onCoreCreated(solrClient);
                    }
                    coreDescriptor.onCoreStarted(solrClient);
                } catch (IOException | SolrServerException e) {
                    if (log.isDebugEnabled()) {
                        log.warn("Error while initializing core {}: {}", coreDescriptor.getCoreName(), e.getMessage(), e);
                    }
                    //noinspection ThrowableResultOfMethodCallIgnored
                    coreInitException.put(coreDescriptor.getCoreName(), e);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted!", e);
            } finally {
                coreLatch.countDown();
            }
        });
    }

    public void shutdown() throws IOException {
    }

    protected abstract SolrClient createSolrClient(String coreName);

    public SolrClient getSolrClient(String coreName) throws SolrServerException {
        try {
            // Wait for the CoreContainer to be online
            awaitInitCompletion();
            // Check for and propagate any exception during CoreContainer initialisation
            if (initException != null) {
                throw new SolrServerException("Exception initializing SolrCoreContainer", initException);
            }

            // Check for and propagate any core-specific exception during CoreContainer initialisation
            final Throwable coreInitException = this.coreInitException.get(coreName);
            if (coreInitException != null) {
                throw new SolrServerException("Exception initializing core " + coreName, coreInitException);
            }

            // Wait for the core-initialisation to be completed
            awaitCoreInitCompletion(coreName);
            // Check for and propagate any core-specific exception during core initialisation
            final Throwable coreInitExceptionDuringCallback = this.coreInitException.get(coreName);
            if (coreInitExceptionDuringCallback != null) {
                throw new SolrServerException("Exception initializing core " + coreName, coreInitExceptionDuringCallback);
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

    protected void awaitCoreInitCompletion(String coreName) throws InterruptedException {
        final CountDownLatch coreLatch = coreInitialized.get(coreName);
        if (coreLatch != null) {
            coreLatch.await();
        } else {
            throw new IllegalArgumentException("Unknown core: " + coreName);
        }
    }

    public SolrClient getSolrClient(SolrCoreDescriptor coreDescriptor) throws SolrServerException {
        return getSolrClient(coreDescriptor.getCoreName());
    }
}
