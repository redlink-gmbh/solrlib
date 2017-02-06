/*
 * Copyright (c) 2017 Redlink GmbH.
 */

import io.redlink.solrlib.SolrCoreContainer;
import io.redlink.solrlib.cloud.SolrCloudConnector;
import io.redlink.solrlib.embedded.EmbeddedCoreContainer;
import io.redlink.solrlib.spring.boot.autoconfigure.SolrLibProperties;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.context.annotation.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@ActiveProfiles("embedded")
@EnableAutoConfiguration
public class SolrEmbeddedTest {

    @ClassRule
    public static TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Autowired
    private SolrCoreContainer coreContainer;

    @Test
    public void testInject() throws Exception {
        Assert.assertNotNull(coreContainer);
    }

    @Test
    public void testType() throws Exception {
        Assert.assertThat(coreContainer, Matchers.instanceOf(EmbeddedCoreContainer.class));
    }

    @Test
    public void testCoreDeployment() throws Exception {
        SolrClient foo = coreContainer.getSolrClient("foo");
        Assert.assertNotNull(foo);

        SolrPingResponse pingResponse = foo.ping();
        Assert.assertEquals(0, pingResponse.getStatus());
    }

    @Configuration
    @Import(TestCoreDesciptorsConfiguration.class)
    static class EmbeddedConfiguration {

        @Bean
        @Primary
        SolrLibProperties solrLibProperties() throws IOException {
            SolrLibProperties properties = new SolrLibProperties();

            properties.setHome(Files.createTempDirectory(temporaryFolder.getRoot().toPath(), "solr-home"));

            return properties;
        }

    }
}
