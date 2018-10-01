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
package io.redlink.solrlib;

import io.redlink.utils.PathUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A SolrCoreDescriptor.
 */
public interface SolrCoreDescriptor {

    default int getNumShards() { return 1; }

    default int getReplicationFactor() { return 1; }

    default String getCoreName() {
        return getClass().getSimpleName();
    }

    void initCoreDirectory(Path coreDir, Path sharedLibDir) throws IOException;

    default void onCoreCreated(SolrClient solrClient) throws IOException, SolrServerException {}

    default void onCoreStarted(SolrClient solrClient) throws IOException, SolrServerException {}

    @SuppressWarnings("squid:S3725")
    static void unpackSolrCoreDir(Path solrCoreBundle, Path solrCoreDir) throws IOException {
        LoggerFactory.getLogger(SolrCoreDescriptor.class).debug("Unpacking SolrCore directory {} to {}", solrCoreBundle, solrCoreDir);
        try (Stream<Path> pathStream = Files.find(solrCoreBundle, Integer.MAX_VALUE,
                (p, a) -> Files.isRegularFile(p)
                        && Files.isReadable(p)
                        && "core.properties".equals(String.valueOf(p.getFileName()))
        )) {
            final Optional<Path> coreProperties = pathStream.min(Comparator.comparingInt(Path::getNameCount));
            final Path sourceDir = coreProperties
                    .orElseThrow(() -> new IllegalArgumentException("Invalid solrCoreBundle '" + solrCoreBundle + "': no core.properties found"))
                    .getParent();

            PathUtils.copyRecursive(sourceDir, solrCoreDir);
        }
    }

    static void unpackSolrCoreZip(Path solrCoreBundle, Path solrHome) throws IOException {
        unpackSolrCoreZip(solrCoreBundle, solrHome, null);
    }

    static void unpackSolrCoreZip(Path solrCoreBundle, Path solrHome, ClassLoader classLoader) throws IOException {
        final String contentType = Files.probeContentType(solrCoreBundle);
        if ("application/zip".equals(contentType) ||
                //fallback if Files.probeContentType(..) fails (such as on Max OS X)
                (contentType == null && StringUtils.endsWithAny(solrCoreBundle.getFileName().toString(), ".zip", ".jar"))) {
            LoggerFactory.getLogger(SolrCoreDescriptor.class).debug("Unpacking SolrCore zip {} to {}", solrCoreBundle, solrHome);
            try (FileSystem fs = FileSystems.newFileSystem(solrCoreBundle, classLoader)) {
                unpackSolrCoreDir(fs.getPath("/"), solrHome);
            }
        } else {
            throw new IllegalArgumentException("Packaged solrCoreBundle '" + solrCoreBundle + "' has unsupported type: " + contentType);
        }
    }

}
