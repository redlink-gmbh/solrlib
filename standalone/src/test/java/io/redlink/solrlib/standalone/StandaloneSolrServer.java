/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package io.redlink.solrlib.standalone;

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

/**
 * Created by jakob on 20.02.17.
 */
public class StandaloneSolrServer extends ExternalResource {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final JettyConfig jettyConfig;
    private Path solrHome;
    private JettySolrRunner jetty;

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

    @Override
    protected void before() throws Throwable {
        super.before();

        solrHome = Files.createTempDirectory("testSolr");
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
            if (solrHome != null) {
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
