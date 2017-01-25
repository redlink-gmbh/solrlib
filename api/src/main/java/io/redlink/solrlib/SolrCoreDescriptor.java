/*
 * Copyright (c) 2016 Redlink GmbH.
 */
package io.redlink.solrlib;

import io.redlink.utils.PathUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
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

    protected final void unpackSolrCoreDir(Path solrCoreBundle, Path solrCoreDir) throws IOException {
        final Optional<Path> solrXml =
                Files.find(solrCoreBundle, Integer.MAX_VALUE,
                        (p, a) -> Files.isRegularFile(p)
                                && Files.isReadable(p)
                                && "core.properties".equals(String.valueOf(p.getFileName()))
                )
                        .sorted((a,b)-> Integer.compare(a.getNameCount(), b.getNameCount()))
                        .findFirst();
        final Path sourceDir = solrXml.orElseThrow(() -> new IllegalArgumentException("Invalid solrCoreBundle '" + solrCoreBundle + "': no solr.xml found"))
                .getParent();

        PathUtils.copyRecursive(sourceDir, solrCoreDir);
    }

    protected final void unpackSolrCoreZip(Path solrCoreBundle, Path solrHome) throws IOException {
        final String contentType = Files.probeContentType(solrCoreBundle);
        if ("application/zip".equals(contentType) ||
                //fallback if Files.probeContentType(..) fails (such as on Max OS X)
                (contentType == null && StringUtils.endsWithAny(solrCoreBundle.getFileName().toString(), ".zip", ".jar"))) {
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
