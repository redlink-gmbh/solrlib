package io.redlink.solrlib.embedded;

import io.redlink.solrlib.SimpleCoreDescriptor;
import io.redlink.solrlib.SolrCoreDescriptor;
import io.redlink.utils.ResourceLoaderUtils;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import static org.junit.Assert.*;

/**
 */
public class EmbeddedCoreContainerTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor() throws Exception {
        new EmbeddedCoreContainer(Collections.<SolrCoreDescriptor>emptySet(), new EmbeddedCoreContainerConfiguration(), null);
    }

    @Test
    public void testInit() throws Exception {
        final Path solrHome = temporaryFolder.newFolder("solr-home").toPath();

        final EmbeddedCoreContainerConfiguration config = new EmbeddedCoreContainerConfiguration();
        config.setHome(solrHome);

        final EmbeddedCoreContainer coreContainer = new EmbeddedCoreContainer(Collections.singleton(
                new SimpleCoreDescriptor("foo", ResourceLoaderUtils.getResourceAsPath("/basic.zip", EmbeddedCoreContainerTest.class))
                        .setNumShards(1).setReplicationFactor(1)
        ), config, null);

        coreContainer.init();
        try {
            assertTrue("solr.xml", Files.exists(solrHome.resolve("solr.xml")));
            assertTrue("foo/core.properties", Files.exists(solrHome.resolve("foo").resolve("core.properties")));

            try {
                coreContainer.init();
                fail("double-init must not succeed!");
            } catch (IllegalStateException e) {
                assertThat(e.getMessage(), Matchers.equalTo("Already initialized!"));
            }
        } finally {
            coreContainer.shutdown();
        }
    }

    @Test
    public void testCreateSolrClient() throws Exception {
        final Path solrHome = temporaryFolder.newFolder("solr-home").toPath();

        final EmbeddedCoreContainerConfiguration config = new EmbeddedCoreContainerConfiguration();
        config.setHome(solrHome);

        final EmbeddedCoreContainer coreContainer = new EmbeddedCoreContainer(Collections.singleton(
                new SimpleCoreDescriptor("foo", ResourceLoaderUtils.getResourceAsPath("/basic.zip", EmbeddedCoreContainerTest.class))
                        .setNumShards(1).setReplicationFactor(1)
        ), config, null);

        try {
            coreContainer.createSolrClient("foo");
            fail("must not create solr-client for non-initialized core-container");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), Matchers.equalTo("CoreContainer not initialized!"));
        }
        coreContainer.init();
        try {
            try {
                coreContainer.createSolrClient("");
                fail("must not create solr-client for empty core-name");
            } catch (IllegalArgumentException ignore) {
            }
            assertNotNull(coreContainer.createSolrClient("foo"));
        } finally {
            coreContainer.shutdown();
        }
    }
}