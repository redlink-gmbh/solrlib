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

import io.redlink.solrlib.SolrCoreContainer;
import io.redlink.solrlib.SolrCoreDescriptor;
import io.redlink.solrlib.spring.boot.autoconfigure.SolrLibCloudAutoconfiguration;
import io.redlink.solrlib.spring.boot.autoconfigure.SolrLibEmbeddedAutoconfiguration;
import io.redlink.solrlib.spring.boot.autoconfigure.SolrLibStandaloneAutoconfiguration;
import io.redlink.solrlib.standalone.SolrServerConnector;
import io.redlink.solrlib.standalone.test.StandaloneSolrServer;
import io.redlink.utils.PathUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(
        classes = TestCoreDesciptorsConfiguration.class,
        initializers = ConfigFileApplicationContextInitializer.class
)
@ActiveProfiles("standalone")
@ImportAutoConfiguration({SolrLibEmbeddedAutoconfiguration.class, SolrLibStandaloneAutoconfiguration.class, SolrLibCloudAutoconfiguration.class})
public class SolrStandaloneIT {

    @ClassRule
    public static StandaloneSolrServer solrServer = createStandaloneServer();

    private static StandaloneSolrServer createStandaloneServer() {
        final String solrHome = System.getProperty("solr.server.home");
        final String solrPort = System.getProperty("solr.server.port");
        Assume.assumeNotNull(solrHome, solrPort);

        final Path homePath = Paths.get(solrHome);
        final int port = Integer.parseInt(solrPort);

        return new StandaloneSolrServer(port, null, homePath) {
            @Override
            protected void before() throws Throwable {
                Files.createDirectories(this.getSolrHome());

                super.before();
            }

            @Override
            protected void after() {
                super.after();

                try {
                    PathUtils.deleteRecursive(this.getSolrHome());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
    }

    @SuppressWarnings("SpringJavaAutowiringInspection")
    @Autowired
    private SolrCoreContainer coreContainer;

    @Autowired
    @Qualifier(TestCoreDesciptorsConfiguration.CORE_NAME)
    private SolrCoreDescriptor solrCoreDescriptor;

    @Test
    public void testInject() throws Exception {
        Assert.assertNotNull(coreContainer);
    }

    @Test
    public void testType() throws Exception {
        Assert.assertThat(coreContainer, Matchers.instanceOf(SolrServerConnector.class));
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
