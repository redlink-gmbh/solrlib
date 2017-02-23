package io.redlink.solrlib.standalone;

import io.redlink.solrlib.SimpleCoreDescriptor;
import io.redlink.solrlib.SolrCoreDescriptor;
import io.redlink.solrlib.standalone.test.StandaloneSolrServer;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.hamcrest.Matchers;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;

import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

/**
 */
public class SolrServerConnectorIT {

    private Logger log = LoggerFactory.getLogger(getClass());

    @ClassRule
    public static StandaloneSolrServer solrServer = new StandaloneSolrServer();

    @ClassRule
    public static TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final SolrServerConnectorConfiguration configuration;


    public SolrServerConnectorIT() throws IOException {
        configuration = new SolrServerConnectorConfiguration();
        configuration.setSolrHome(temporaryFolder.newFolder("solr-home").toPath());
        configuration.setSolrUrl(solrServer.getBaseUrl());
        log.info("Testing against external server {}", solrServer.getBaseUrl());
    }

    @Test
    public void testCheckInit() throws Exception {
        final SolrCoreDescriptor basic = spy(SimpleCoreDescriptor.createFromResource("simple1", "/basic.zip", this.getClass()));

        final SolrServerConnector ssc = new SolrServerConnector(Collections.singleton(basic), configuration);
        ssc.initialize();
        try (SolrClient solrClient = ssc.getSolrClient(basic)) {
            assertThat(solrClient.ping().getStatus(), Matchers.equalTo(0));

            solrClient.add(createSolrDoc("id1", "title1"));
            solrClient.commit();
        }
        verify(basic, times(1)).initCoreDirectory(any(), any());
        verify(basic, times(1)).onCoreCreated(any());
        verify(basic, times(1)).onCoreStarted(any());

        final SolrServerConnector ssc2 = new SolrServerConnector(Collections.singleton(basic), configuration);
        ssc2.initialize();
        try (SolrClient solrClient = ssc2.getSolrClient(basic)) {
            assertThat(solrClient.ping().getStatus(), Matchers.equalTo(0));
        }
        verify(basic, times(2)).initCoreDirectory(any(), any());
        verify(basic, times(1)).onCoreCreated(any());
        verify(basic, times(2)).onCoreStarted(any());

    }

    private SolrInputDocument createSolrDoc(String id, String... fields) {
        final SolrInputDocument inputDocument = new SolrInputDocument();
        inputDocument.setField("id", id);

        String[] fieldNames = {"title", "description", "category"};
        for (int i = 0; i < fields.length && i < fieldNames.length; i++) {
            inputDocument.setField(fieldNames[i] + "_s", fields[i]);
        }

        return inputDocument;
    }
}