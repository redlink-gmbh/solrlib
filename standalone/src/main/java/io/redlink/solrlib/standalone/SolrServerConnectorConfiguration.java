/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package io.redlink.solrlib.standalone;

import java.nio.file.Path;

/**
 * Configuration-Settings for a {@link SolrServerConnector}.
 */
public class SolrServerConnectorConfiguration {
    private String prefix;
    private String solrUrl;
    private Path solrHome;

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getSolrUrl() {
        return solrUrl;
    }

    public void setSolrUrl(String solrUrl) {
        this.solrUrl = solrUrl;
    }

    public Path getSolrHome() {
        return solrHome;
    }

    public void setSolrHome(Path solrHome) {
        this.solrHome = solrHome;
    }
}
