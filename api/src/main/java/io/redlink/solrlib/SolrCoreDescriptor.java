/*
 * Copyright (c) 2016 Redlink GmbH.
 */
package io.redlink.solrlib;

import io.redlink.utils.PathUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;

/**
 * A SolrCoreDescriptor.
 */
public abstract class SolrCoreDescriptor {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    public int getNumShards() { return 1; }

    public int getReplicationFactor() { return 1; }

    public String getCoreName() {
        return getClass().getSimpleName();
    }

    public abstract void initCoreDirectory(Path coreDir, Path sharedLibDir) throws IOException;

    public void onCoreCreated(SolrClient solrClient) throws IOException, SolrServerException {}

    public void onCoreStarted(SolrClient solrClient) throws IOException, SolrServerException {}

    protected final void unpackSolrCoreDir(Path solrCoreBundle, Path solrCoreDir) throws IOException {
        log.debug("Unpacking SolrCore directory {} to {}", solrCoreBundle, solrCoreDir);
        final Optional<Path> coreProperties =
                Files.find(solrCoreBundle, Integer.MAX_VALUE,
                        (p, a) -> Files.isRegularFile(p)
                                && Files.isReadable(p)
                                && "core.properties".equals(String.valueOf(p.getFileName()))
                )
                        .sorted((a,b)-> Integer.compare(a.getNameCount(), b.getNameCount()))
                        .findFirst();
        final Path sourceDir = coreProperties.orElseThrow(() -> new IllegalArgumentException("Invalid solrCoreBundle '" + solrCoreBundle + "': no core.properties found"))
                .getParent();

        PathUtils.copyRecursive(sourceDir, solrCoreDir);
    }

    protected final void unpackSolrCoreZip(Path solrCoreBundle, Path solrHome) throws IOException {
        final String contentType = Files.probeContentType(solrCoreBundle);
        if ("application/zip".equals(contentType) ||
                //fallback if Files.probeContentType(..) fails (such as on Max OS X)
                (contentType == null && StringUtils.endsWithAny(solrCoreBundle.getFileName().toString(), ".zip", ".jar"))) {
            log.debug("Unpacking SolrCore zip {} to {}", solrCoreBundle, solrHome);
            try (FileSystem fs = FileSystems.newFileSystem(solrCoreBundle, getClass().getClassLoader())) {
                unpackSolrCoreDir(fs.getPath("/"), solrHome);
            }
        } else {
            throw new IllegalArgumentException("Packaged solrCoreBundle '" + solrCoreBundle + "' has unsupported type: " + contentType);
        }
    }

    @Override
    public String toString() {
        return String.format(Locale.ENGLISH, "CoreDescriptor '%s'", getCoreName());
    }
}
