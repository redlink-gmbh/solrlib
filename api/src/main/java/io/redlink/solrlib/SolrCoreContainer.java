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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 */
public abstract class SolrCoreContainer {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    private final Optional<ExecutorService> executorService;
    private final CountDownLatch startupComplete;
    private final Map<String, CountDownLatch> coreInitialized;
    private final AtomicBoolean initStarted;
    private Exception initException = null;
    protected final Map<String, Throwable> coreInitExceptions = new HashMap<>();
    protected final Set<SolrCoreDescriptor> coreDescriptors;
    protected final Map<String, SolrCoreDescriptor> availableCores = new HashMap<>();

    public SolrCoreContainer(Set<SolrCoreDescriptor> coreDescriptors, ExecutorService executorService) {
        this.executorService = Optional.ofNullable(executorService);
        startupComplete = new CountDownLatch(1);
        coreInitialized = new HashMap<>();
        initStarted = new AtomicBoolean(false);
        this.coreDescriptors = coreDescriptors;
    }

    @SuppressWarnings("squid:S3776")
    public final void initialize() {
        if (initStarted.compareAndSet(false, true)) {
            final long initStart = System.currentTimeMillis();
            log.debug("Initializing SolrCoreContainer");
            final ExecutorService lEexecutorService = this.executorService.orElseGet(Executors::newSingleThreadExecutor);
            lEexecutorService
                    .execute(() -> {
                        try {
                            init(lEexecutorService);
                        } catch (IOException | SolrServerException e) {
                            if (log.isDebugEnabled()) {
                                log.error("Error while initializing SolrCoreContainer: {}", e.getMessage(), e);
                            } else {
                                log.error("Error while initializing SolrCoreContainer: {}", e.getMessage());
                            }
                            initException = e;
                        } catch (final Exception t) {
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
                                lEexecutorService.shutdown();
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

    protected abstract void init(ExecutorService executorService) throws IOException, SolrServerException;

    protected void scheduleCoreInit(ExecutorService executorService, SolrCoreDescriptor coreDescriptor, boolean newCore) {
        final CountDownLatch coreLatch = coreInitialized.computeIfAbsent(coreDescriptor.getCoreName(), s -> new CountDownLatch(1));
        executorService.execute(() -> {
            try {
                awaitInitCompletion();
                initCore(coreDescriptor, newCore);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted!", e);
            } finally {
                coreLatch.countDown();
            }
        });
    }

    private void initCore(SolrCoreDescriptor coreDescriptor, boolean isNewCore) {
        try (SolrClient solrClient = createSolrClient(coreDescriptor.getCoreName())) {
            if (isNewCore) {
                coreDescriptor.onCoreCreated(solrClient);
            }
            coreDescriptor.onCoreStarted(solrClient);
        } catch (IOException | SolrServerException e) {
            if (log.isDebugEnabled()) {
                log.warn("Error while initializing core {}: {}", coreDescriptor.getCoreName(), e.getMessage(), e);
            }
            //noinspection ThrowableResultOfMethodCallIgnored
            coreInitExceptions.put(coreDescriptor.getCoreName(), e);
        }
    }

    public void shutdown() throws IOException {
    }

    protected abstract SolrClient createSolrClient(String coreName);

    /**
     * Get a SolrClient for the provided SolrCoreDescriptor.
     * <strong>Note:</strong> the caller is responsible for closing the returned SolrClient to avoid resource leakage.
     * @param coreName the core to connect to
     * @return a SolrClient
     * @throws SolrServerException if the initialisation of the requested core failed
     */
    public SolrClient getSolrClient(String coreName) throws SolrServerException {
        try {
            // Wait for the CoreContainer to be online
            awaitInitCompletion();
            // Check for and propagate any exception during CoreContainer initialisation
            if (initException != null) {
                throw new SolrServerException("Exception initializing SolrCoreContainer", initException);
            }

            // Check for and propagate any core-specific exception during CoreContainer initialisation
            final Throwable coreInitException = this.coreInitExceptions.get(coreName);
            if (coreInitException != null) {
                throw new SolrServerException("Exception initializing core " + coreName, coreInitException);
            }

            // Wait for the core-initialisation to be completed
            awaitCoreInitCompletion(coreName);
            // Check for and propagate any core-specific exception during core initialisation
            final Throwable coreInitExceptionDuringCallback = this.coreInitExceptions.get(coreName);
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

    /**
     * Get a SolrClient for the provided SolrCoreDescriptor.
     * <strong>Note:</strong> the caller is responsible for closing the returned SolrClient to avoid resource leakage.
     * @param coreDescriptor the core to connect to
     * @return a SolrClient
     * @throws SolrServerException if the initialisation of the requested core failed
     */
    public SolrClient getSolrClient(SolrCoreDescriptor coreDescriptor) throws SolrServerException {
        return getSolrClient(coreDescriptor.getCoreName());
    }

    /**
     * Non-blocking check if startup of the SolrCoreContainer is complete.
     * @return {@code true} if startup is complete.
     */
    public boolean isStartupComplete() {
        try {
            return startupComplete.await(-1, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            // This should never happen...
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Check if the given core is available (i.e. initialized)
     */
    public boolean isCoreAvailable(String coreName) {
        return availableCores.containsKey(coreName);
    }

    /**
     * Check if the given core is available (i.e. initialized)
     */
    public boolean isCoreAvailable(SolrCoreDescriptor coreDescriptor) {
        return isCoreAvailable(coreDescriptor.getCoreName());
    }

}
