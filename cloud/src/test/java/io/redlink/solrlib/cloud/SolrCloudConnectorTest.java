package io.redlink.solrlib.cloud;

import io.redlink.solrlib.SolrCoreDescriptor;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.util.Collections;
import java.util.UUID;

import static org.junit.Assert.*;

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
}