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

package io.redlink.solrlib.cloud;

import com.google.common.collect.Sets;
import io.redlink.solrlib.SolrCoreDescriptor;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

/**
 */
public class SolrCloudConnectorTest {

    @Test
    public void testRemoteName() throws Exception {
        final String prefix = UUID.randomUUID().toString();
        SolrCloudConnectorConfiguration config = new SolrCloudConnectorConfiguration();
        config.setPrefix(prefix);
        SolrCloudConnector connector = new SolrCloudConnector(Collections.<SolrCoreDescriptor>emptySet(), config, null);
        for (int i = 0; i < 10; i++) {
            final String coreName = UUID.randomUUID().toString();
            assertThat(connector.createRemoteName(coreName), Matchers.allOf(Matchers.startsWith(prefix), Matchers.endsWith(coreName), Matchers.equalTo(prefix + coreName)));
        }
    }

    @Test
    public void testEmptyPrefix() throws Exception {
        SolrCloudConnectorConfiguration config = new SolrCloudConnectorConfiguration();
        SolrCloudConnector connector = new SolrCloudConnector(Collections.<SolrCoreDescriptor>emptySet(), config, null);
        for (int i = 0; i < 10; i++) {
            final String coreName = UUID.randomUUID().toString();
            assertThat(connector.createRemoteName(coreName), Matchers.equalTo(coreName));
        }
    }

    @Test
    public void testCreateClient() throws Exception {
        final String zkConnection = UUID.randomUUID().toString();
        SolrCloudConnectorConfiguration config = new SolrCloudConnectorConfiguration();
        config.setZkConnection(zkConnection);
        SolrCloudConnector connector = new SolrCloudConnector(Collections.<SolrCoreDescriptor>emptySet(), config, null);

        CloudSolrClient c1 = connector.createSolrClient();
        assertThat(c1.getZkHost(), Matchers.equalTo(zkConnection));

        for (int i = 0; i < 10; i++) {
            final String coreName = UUID.randomUUID().toString();
            SolrClient cI = connector.createSolrClient(coreName);
            assertThat(cI, Matchers.instanceOf(CloudSolrClient.class));
            CloudSolrClient c2 = (CloudSolrClient) cI;
            assertThat(c2.getZkHost(), Matchers.equalTo(zkConnection));
            assertThat(c2.getDefaultCollection(), Matchers.equalTo(coreName));
        }
    }

    @Test
    public void testInitCore() throws Exception {
        final ExecutorService exec = Executors.newSingleThreadExecutor();
        try {
            final String zkConnection = UUID.randomUUID().toString();
            final SolrCloudConnectorConfiguration config = new SolrCloudConnectorConfiguration();
            config.setZkConnection(zkConnection);

            final CloudSolrClient solrClient = mock(CloudSolrClient.class);
            final SimpleOrderedMap<Object> collectionsListResponse = new SimpleOrderedMap<>();
            collectionsListResponse.add("collections", Collections.singletonList("core1"));

            when(solrClient.request(any(CollectionAdminRequest.List.class), eq(null))).thenReturn(collectionsListResponse);

            final SolrCoreDescriptor solrCoreDescriptor1 = mock(SolrCoreDescriptor.class);
            when(solrCoreDescriptor1.getCoreName()).thenReturn("core1");
            final SolrCoreDescriptor solrCoreDescriptor2 = mock(SolrCoreDescriptor.class);
            when(solrCoreDescriptor2.getCoreName()).thenReturn("core2");
            final SolrCloudConnector connector = spy(new SolrCloudConnector(Sets.newHashSet(solrCoreDescriptor1, solrCoreDescriptor2), config, exec));
            when(connector.createSolrClient()).thenReturn(solrClient);

            connector.initialize();
            assertNotNull(connector.getSolrClient(solrCoreDescriptor1));
            assertNotNull(connector.getSolrClient(solrCoreDescriptor2));

            verify(solrCoreDescriptor1, times(1)).initCoreDirectory(any(), any());
            verify(solrCoreDescriptor1, never()).onCoreCreated(any());
            verify(solrCoreDescriptor1, times(1)).onCoreStarted(any());

            verify(solrCoreDescriptor2, times(1)).initCoreDirectory(any(), any());
            verify(solrCoreDescriptor2, times(1)).onCoreCreated(any());
            verify(solrCoreDescriptor2, times(1)).onCoreStarted(any());

        } finally {
            exec.shutdownNow();
        }
    }
}