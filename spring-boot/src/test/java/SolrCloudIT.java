/*
 * Copyright (c) 2017 Redlink GmbH.
 */

import io.redlink.solrlib.SolrCoreContainer;
import io.redlink.solrlib.SolrCoreDescriptor;
import io.redlink.solrlib.cloud.SolrCloudConnector;
import io.redlink.solrlib.spring.boot.autoconfigure.SolrLibCloudAutoconfiguration;
import io.redlink.solrlib.spring.boot.autoconfigure.SolrLibEmbeddedAutoconfiguration;
import io.redlink.solrlib.spring.boot.autoconfigure.SolrLibProperties;
import io.redlink.solrlib.spring.boot.autoconfigure.SolrLibStandaloneAutoconfiguration;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.concurrent.TimeUnit;

/**
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(
        classes = TestCoreDesciptorsConfiguration.class,
        initializers = ConfigFileApplicationContextInitializer.class
)
@ActiveProfiles("cloud")
@ImportAutoConfiguration({SolrLibEmbeddedAutoconfiguration.class, SolrLibStandaloneAutoconfiguration.class, SolrLibCloudAutoconfiguration.class})
public class SolrCloudIT {

    @SuppressWarnings("SpringJavaAutowiringInspection")
    @Autowired
    private SolrCoreContainer coreContainer;

    @SuppressWarnings("SpringJavaAutowiringInspection")
    @Autowired
    private SolrLibProperties solrLibProperties;

    @Autowired
    @Qualifier(TestCoreDesciptorsConfiguration.CORE_NAME)
    private SolrCoreDescriptor solrCoreDescriptor;

    @Before
    public void setUp() throws Exception {
        try (CloudSolrClient client = new CloudSolrClient.Builder().withZkHost(solrLibProperties.getZkConnection()).build()) {
            client.connect(250, TimeUnit.MILLISECONDS);
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
    public void testCoreDeployment_1() throws Exception {
        SolrClient foo = coreContainer.getSolrClient(TestCoreDesciptorsConfiguration.CORE_NAME);
        Assert.assertNotNull(foo);

        SolrPingResponse pingResponse = foo.ping();
        Assert.assertEquals(0, pingResponse.getStatus());
    }

    @Test
    public void testCoreDeployment_2() throws Exception {
        SolrClient foo = coreContainer.getSolrClient(solrCoreDescriptor);
        Assert.assertNotNull(foo);

        SolrPingResponse pingResponse = foo.ping();
        Assert.assertEquals(0, pingResponse.getStatus());
    }


}
