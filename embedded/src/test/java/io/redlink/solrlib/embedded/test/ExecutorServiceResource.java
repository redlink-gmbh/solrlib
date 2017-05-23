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
