package io.redlink.solrlib;

import org.apache.solr.client.solrj.SolrClient;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Collections;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 */
public class SolrCoreContainerTest {


    @Test
    public void testInitialize() throws Exception {
        final SolrCoreContainer coreContainer = Mockito.spy(new SolrCoreContainer(Collections.emptySet(), null) {
            @Override
            protected void init() throws IOException {
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
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
        Mockito.verify(coreContainer, Mockito.times(1)).init();
        coreContainer.initialize();
    }

    @Test
    public void testCreateSolrClient() throws Exception {
        final SolrCoreContainer coreContainer = new SolrCoreContainer(Collections.emptySet(), null) {

            SolrClient solrClient = null;

            @Override
            protected void init() throws IOException {
                try {
                    Thread.sleep(1500);
                    solrClient = Mockito.mock(SolrClient.class);
                } catch (InterruptedException e) {
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