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
package io.redlink.solrlib.standalone.test;

import com.google.common.base.Preconditions;
import io.redlink.utils.PathUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.embedded.JettyConfig;
import org.apache.solr.client.solrj.embedded.JettySolrRunner;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Start and stop a Solr-Server, wrapped in an {@link ExternalResource}.
 */
public class StandaloneSolrServer extends ExternalResource {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final JettyConfig jettyConfig;
    private Path solrHome;
    private JettySolrRunner jetty;
    private boolean deleteSolrHome = true;

    public StandaloneSolrServer() {
        this(-1, null);
    }

    public StandaloneSolrServer(int port, String context) {
        final JettyConfig.Builder builder = JettyConfig.builder()
                .setContext(StringUtils.defaultString(context, "/solr"));
        if (port > 0)
            builder.setPort(port);
        jettyConfig = builder.build();
    }

    public StandaloneSolrServer(int port, String context, Path solrHome) {
        this(port, context);
        this.solrHome = solrHome;
    }

    @Override
    protected void before() throws Throwable {
        super.before();

        if (Objects.isNull(solrHome)) {
            solrHome = Files.createTempDirectory("testSolr");
        } else {
            Files.createDirectories(solrHome);
            deleteSolrHome = false;
        }
        try (PrintStream solrXml = new PrintStream(Files.newOutputStream(solrHome.resolve("solr.xml")))) {
            solrXml.println("<solr></solr>");
        }

        jetty = new JettySolrRunner(solrHome.toAbsolutePath().toString(), jettyConfig);
        jetty.start();
        logger.warn("Started StandaloneSolrServer {}", getBaseUrl());
    }

    @Override
    protected void after() {
        try {
            if (jetty != null) {
                logger.warn("Stopping StandaloneSolrServer");
                jetty.stop();
            }
            if (deleteSolrHome && solrHome != null) {
                logger.warn("Cleaning Up solr-home {}", solrHome);
                PathUtils.deleteRecursive(solrHome);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            super.after();
        }
    }

    public String getBaseUrl() {
        Preconditions.checkState(jettyConfig != null);
        Preconditions.checkState(jetty != null);

        return "http://localhost:" + jetty.getLocalPort() + jettyConfig.context;
    }

    public Path getSolrHome() {
        return solrHome;
    }
}
