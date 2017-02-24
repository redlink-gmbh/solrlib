/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package io.redlink.solrlib.embedded.test;

import org.junit.rules.ExternalResource;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 */
public class ExecutorServiceResource extends ExternalResource {

    private final Optional<Supplier<ExecutorService>> executorServiceSupplier;
    private ExecutorService executorService;

    public ExecutorServiceResource(Supplier<ExecutorService> executorService) {
        this.executorServiceSupplier = Optional.ofNullable(executorService);
    }

    public ExecutorServiceResource() {
        this(null);
    }

    @Override
    protected void before() throws Throwable {
        executorService = executorServiceSupplier.orElse(Executors::newSingleThreadExecutor).get();
    }

    @Override
    protected void after() {
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    public ExecutorService get() {
        return executorService;
    }


}
