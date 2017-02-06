/*
 * Copyright (c) 2017 Redlink GmbH.
 */

import io.redlink.solrlib.SolrCoreContainer;
import io.redlink.solrlib.SolrCoreDescriptor;
import io.redlink.solrlib.cloud.SolrCloudConnector;
import io.redlink.solrlib.spring.boot.autoconfigure.SolrLibProperties;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.client.solrj.request.ConfigSetAdminRequest;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.hamcrest.Matchers;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.Set;

/**
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(
        classes = TestCoreDesciptorsConfiguration.class,
        initializers = ConfigFileApplicationContextInitializer.class
)
@ActiveProfiles("cloud")
@EnableAutoConfiguration
public class SolrCloudTest {

    @Autowired
    private SolrCoreContainer coreContainer;

    @Autowired
    private SolrLibProperties solrLibProperties;

    @Before
    public void setUp() throws Exception {
        try (CloudSolrClient client = new CloudSolrClient.Builder().withZkHost(solrLibProperties.getZkConnection()).build()) {
            client.connect();
            Assume.assumeTrue(client.getZkStateReader().getZkClient().isConnected());
        } catch (final Throwable t) {
            Assume.assumeNoException(t);
        }
    }

    @Test
    public void testInject() throws Exception {
        Assert.assertNotNull(coreContainer);
    }

    @Test
    public void testType() throws Exception {
        Assert.assertThat(coreContainer, Matchers.instanceOf(SolrCloudConnector.class));
    }

    @Test
    public void testCoreDeployment() throws Exception {
        SolrClient foo = coreContainer.getSolrClient("foo");
        Assert.assertNotNull(foo);

        SolrPingResponse pingResponse = foo.ping();
        Assert.assertEquals(0, pingResponse.getStatus());
    }


}
