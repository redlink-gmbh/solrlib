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

package io.redlink.solrlib.embedded;

import io.redlink.solrlib.SimpleCoreDescriptor;
import io.redlink.solrlib.SolrCoreContainer;
import io.redlink.utils.ResourceLoaderUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Path;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 */
public class EmbeddedCoreContainerIT {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private SolrCoreContainer coreContainer;
    private SimpleCoreDescriptor coreDescriptor;

    @Before
    public void setUp() throws Exception {
        final Path solrHome = temporaryFolder.newFolder("solr-home").toPath();

        final EmbeddedCoreContainerConfiguration config = new EmbeddedCoreContainerConfiguration();
        config.setHome(solrHome);

        coreDescriptor = new SimpleCoreDescriptor("foo", ResourceLoaderUtils.getResourceAsPath("/basic.zip", EmbeddedCoreContainerTest.class))
                .setNumShards(1).setReplicationFactor(1);
        coreContainer = new EmbeddedCoreContainer(Collections.singleton(coreDescriptor), config, null);
        coreContainer.initialize();
    }

    @After
    public void tearDown() throws Exception {
        if (coreContainer != null) coreContainer.shutdown();
    }

    @Test
    public void testPing() throws Exception {
        try (SolrClient solrClient = coreContainer.getSolrClient(coreDescriptor)) {
            assertNotNull(solrClient);
            assertEquals("ping", 0, solrClient.ping().getStatus());
        }
    }
}