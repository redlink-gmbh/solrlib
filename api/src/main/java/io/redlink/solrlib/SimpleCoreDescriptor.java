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

import io.redlink.utils.ResourceLoaderUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * SimpleCoreDescriptor that unpacks a core-bundle from the provided {@link Path}.
 */
public class SimpleCoreDescriptor implements SolrCoreDescriptor {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final String coreName;
    private final Path coreBundle;
    private int numShards, replicationFactor;

    public SimpleCoreDescriptor(String coreName, Path coreBundle) {
        this.coreName = coreName;
        this.coreBundle = coreBundle;
        numShards = 1;
        replicationFactor = 1;
    }

    @Override
    public String getCoreName() {
        return coreName;
    }

    @Override
    public void initCoreDirectory(Path coreDir, Path sharedLibDir) throws IOException {
        log.debug("{}: initializing core-dir {}", coreName, coreDir);
        if (Files.isDirectory(coreBundle)) {
            SolrCoreDescriptor.unpackSolrCoreDir(coreBundle, coreDir);
        } else {
            SolrCoreDescriptor.unpackSolrCoreZip(coreBundle, coreDir);
        }
    }

    @Override
    public int getNumShards() {
        return numShards;
    }

    @Override
    public int getReplicationFactor() {
        return replicationFactor;
    }

    public SimpleCoreDescriptor setNumShards(int numShards) {
        this.numShards = numShards;
        return this;
    }

    public SimpleCoreDescriptor setReplicationFactor(int replicationFactor) {
        this.replicationFactor = replicationFactor;
        return this;
    }

    /**
     * Create a {@link SolrCoreDescriptor} from the provided classpath-resource.
     * @param coreName the core-name
     * @param resource the solr-core bundle on the classpath
     * @param contextObject the object to retrieve the resource from
     *
     * @see Object#getClass()
     */
    public static SolrCoreDescriptor createFromResource(String coreName, String resource, Object contextObject) {
        return createFromResource(coreName, resource, contextObject.getClass());
    }

    /**
     * Create a {@link SolrCoreDescriptor} from the provided classpath-resource.
     * @param coreName the core-name
     * @param resource the solr-core bundle on the classpath
     * @param clazz the Class to retrieve the resource from
     *
     * @see Class#getResource(String)
     */
    public static SolrCoreDescriptor createFromResource(String coreName, String resource, Class<?> clazz) {
        return new SimpleCoreDescriptor(coreName, ResourceLoaderUtils.getResourceAsPath(resource, clazz));
    }

    /**
     * Create a {@link SolrCoreDescriptor} from the provided classpath-resource.
     * @param coreName the core-name
     * @param resource the solr-core bundle on the classpath
     * @param classLoader the {@link ClassLoader} to load the resource
     *
     * @see ClassLoader#getResource(String)
     */
    public static SolrCoreDescriptor createFromResource(String coreName, String resource, ClassLoader classLoader) {
        return new SimpleCoreDescriptor(coreName, ResourceLoaderUtils.getResourceAsPath(resource, classLoader));
    }

}
