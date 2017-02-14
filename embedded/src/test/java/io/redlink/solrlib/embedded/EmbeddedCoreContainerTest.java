package io.redlink.solrlib.embedded;

import io.redlink.solrlib.SimpleCoreDescriptor;
import io.redlink.solrlib.embedded.test.ExecutorServiceResource;
import io.redlink.utils.ResourceLoaderUtils;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 */
public class EmbeddedCoreContainerTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public ExecutorServiceResource executorService = new ExecutorServiceResource();

    @Test
    public void testConstructor() throws Exception {
        final EmbeddedCoreContainer coreContainer = new EmbeddedCoreContainer(Collections.emptySet(), new EmbeddedCoreContainerConfiguration(), null);
        assertNotNull(coreContainer);
    }

    @Test
    public void testInit() throws Exception {
        final Path solrHome = temporaryFolder.newFolder("solr-home").toPath();

        final EmbeddedCoreContainerConfiguration config = new EmbeddedCoreContainerConfiguration();
        config.setHome(solrHome);

        final SimpleCoreDescriptor foo = spy(new SimpleCoreDescriptor("foo", ResourceLoaderUtils.getResourceAsPath("/basic.zip", EmbeddedCoreContainerTest.class))
                .setNumShards(1).setReplicationFactor(1));

        final EmbeddedCoreContainer coreContainer = spy(new EmbeddedCoreContainer(Collections.singleton(foo), config, null));

        coreContainer.init(executorService.get());
        try {
            assertTrue("solr.xml", Files.exists(solrHome.resolve("solr.xml")));
            assertTrue("foo/core.properties", Files.exists(solrHome.resolve("foo").resolve("core.properties")));

            try {
                coreContainer.init(executorService.get());
                fail("double-init must not succeed!");
            } catch (IllegalStateException e) {
                assertThat(e.getMessage(), Matchers.equalTo("Already initialized!"));
            }
        } finally {
            coreContainer.shutdown();
        }

        verify(foo, times(1)).initCoreDirectory(any(), any());
        verify(coreContainer, times(1)).scheduleCoreInit(executorService.get(), foo, true);
    }

    @Test
    public void testCreateSolrClient() throws Exception {
        final Path solrHome = temporaryFolder.newFolder("solr-home").toPath();

        final EmbeddedCoreContainerConfiguration config = new EmbeddedCoreContainerConfiguration();
        config.setHome(solrHome);

        final String coreName = "foo";
        final EmbeddedCoreContainer coreContainer = new EmbeddedCoreContainer(Collections.singleton(
                new SimpleCoreDescriptor(coreName, ResourceLoaderUtils.getResourceAsPath("/basic.zip", EmbeddedCoreContainerTest.class))
                        .setNumShards(1).setReplicationFactor(1)
        ), config, null);

        try {
            coreContainer.createSolrClient(coreName);
            fail("must not create solr-client for non-initialized core-container");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), Matchers.equalTo("CoreContainer not initialized!"));
        }
        coreContainer.init(executorService.get());
        try {
            try {
                coreContainer.createSolrClient("");
                fail("must not create solr-client for empty core-name");
            } catch (IllegalArgumentException ignore) {
            }
            assertNotNull(coreContainer.createSolrClient(coreName));
        } finally {
            coreContainer.shutdown();
        }
    }

}