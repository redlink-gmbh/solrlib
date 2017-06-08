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
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.ExecutorService;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 */
public class SolrCoreContainerTest {


    @Test
    public void testInitialize() throws Exception {
        final SolrCoreContainer coreContainer = Mockito.spy(new SolrCoreContainer(Collections.emptySet(), null) {
            @Override
            protected void init(ExecutorService executorService) throws IOException {
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException(e);
                }
            }

            @Override
            protected SolrClient createSolrClient(String coreName) {
                return null;
            }
        });

        coreContainer.initialize();
        coreContainer.awaitInitCompletion();
        Mockito.verify(coreContainer, Mockito.times(1)).init(Mockito.any());
        coreContainer.initialize();
        Mockito.verify(coreContainer, Mockito.times(1)).init(Mockito.any());
    }

    @Test
    public void testInitCore() throws Exception {
        final SolrCoreDescriptor coreDescriptor = Mockito.mock(SolrCoreDescriptor.class);
        Mockito.when(coreDescriptor.getCoreName()).thenReturn("mock");
        final SolrCoreContainer coreContainer = Mockito.spy(new SolrCoreContainer(Collections.singleton(coreDescriptor), null) {
            @Override
            protected void init(ExecutorService executorService) throws IOException {
                try {
                    Thread.sleep(500);
                    for (SolrCoreDescriptor coreDescriptor : coreDescriptors) {

                        coreDescriptor.initCoreDirectory(null, null);
                        Thread.sleep(500);
                        scheduleCoreInit(executorService, coreDescriptor, true);
                        Thread.sleep(500);
                        availableCores.put(coreDescriptor.getCoreName(), coreDescriptor);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException(e);
                }
            }

            @Override
            protected SolrClient createSolrClient(String coreName) {
                return null;
            }
        });

        coreContainer.initialize();
        coreContainer.awaitInitCompletion();
        coreContainer.awaitCoreInitCompletion("mock");

        Mockito.verify(coreDescriptor, Mockito.times(1)).initCoreDirectory(null, null);
        Mockito.verify(coreDescriptor, Mockito.times(1)).onCoreCreated(Mockito.any());
        Mockito.verify(coreDescriptor, Mockito.times(1)).onCoreStarted(Mockito.any());

        Assert.assertTrue(coreContainer.isCoreAvailable(coreDescriptor));
        Assert.assertTrue(coreContainer.isCoreAvailable("mock"));
    }

    @Test
    public void testCreateSolrClient() throws Exception {
        final SolrCoreContainer coreContainer = new SolrCoreContainer(Collections.emptySet(), null) {

            SolrClient solrClient = null;

            @Override
            protected void init(ExecutorService executorService) throws IOException {
                try {
                    Thread.sleep(1500);
                    solrClient = Mockito.mock(SolrClient.class);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException(e);
                }
            }

            @Override
            protected SolrClient createSolrClient(String coreName) {
                return solrClient;
            }
        };

        assertNull(coreContainer.createSolrClient(""));
        coreContainer.initialize();
        assertNull(coreContainer.createSolrClient(""));
        coreContainer.awaitInitCompletion();
        assertNotNull(coreContainer.createSolrClient(""));

    }
}