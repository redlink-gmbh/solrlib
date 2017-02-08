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