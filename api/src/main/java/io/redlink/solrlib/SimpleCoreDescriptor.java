/*
 * Copyright (c) 2016 Redlink GmbH.
 */
package io.redlink.solrlib;

import io.redlink.utils.ResourceLoaderUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * SimpleCoreDescriptor that unpacks a core-bundle from the provided {@link Path}.
 */
public class SimpleCoreDescriptor extends SolrCoreDescriptor {

    private final String coreName;
    private final Path coreBundle;

    public SimpleCoreDescriptor(String coreName, Path coreBundle) {
        this.coreName = coreName;
        this.coreBundle = coreBundle;
    }

    @Override
    public String getCoreName() {
        return coreName;
    }

    @Override
    public void initCoreDirectory(Path coreDir, Path sharedLibDir) throws IOException {
        if (Files.isDirectory(coreBundle)) {
            unpackSolrCoreDir(coreBundle, coreDir);
        } else {
            unpackSolrCoreZip(coreBundle, coreDir);
        }
    }

    /**
     * Create a {@link SolrCoreDescriptor} from the provided classpath-resource.
     * @param coreName the core-name
     * @param resource the solr-core bundle on the classpath
     * @param contextObject the object to retrieve the resource from
     *
     * @see Object#getClass()
     */
    public static SolrCoreDescriptor createFromResource(String coreName, String resource, Object contextObject) throws IOException {
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
    public static SolrCoreDescriptor createFromResource(String coreName, String resource, Class<?> clazz) throws IOException {
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
    public static SolrCoreDescriptor createFromResource(String coreName, String resource, ClassLoader classLoader) throws IOException {
        return new SimpleCoreDescriptor(coreName, ResourceLoaderUtils.getResourceAsPath(resource, classLoader));
    }

}
