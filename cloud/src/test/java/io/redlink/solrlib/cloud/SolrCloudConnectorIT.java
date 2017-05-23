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

import io.redlink.solrlib.SimpleCoreDescriptor;
import io.redlink.solrlib.SolrCoreDescriptor;
import io.redlink.utils.ResourceLoaderUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.hamcrest.Matchers;
import org.junit.*;

import java.util.Collections;

/**
 *
 */
public class SolrCloudConnectorIT {

    private static SolrCloudConnector coreContainer;
    private static SolrCoreDescriptor coreDescriptor;

    @BeforeClass
    public static void setUp() throws Exception {
        String zkString = System.getProperty("zkConnection");
        Assume.assumeThat("No zkConnection", zkString, Matchers.allOf(Matchers.notNullValue(), Matchers.not(Matchers.isEmptyString())));

        final SolrCloudConnectorConfiguration config = new SolrCloudConnectorConfiguration();
        config.setZkConnection(zkString);
        config.setMaxShardsPerNode(2);
        config.setPrefix("integrationTest_");

        coreDescriptor = new SimpleCoreDescriptor("foo", ResourceLoaderUtils.getResourceAsPath("/basic.zip", SolrCloudConnectorIT.class))
                .setNumShards(1).setReplicationFactor(1);

        coreContainer = new SolrCloudConnector(Collections.singleton(coreDescriptor), config);
        coreContainer.initialize();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (coreContainer != null) coreContainer.shutdown();
    }

    @Test
    public void testPing() throws Exception {
        try (SolrClient solrClient = coreContainer.getSolrClient(coreDescriptor)) {
            Assert.assertThat("ping", solrClient.ping().getStatus(), Matchers.equalTo(0));
        }
    }
}