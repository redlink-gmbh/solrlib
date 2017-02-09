/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package io.redlink.solrlib.embedded;

import java.nio.file.Path;

/**
 */
public class EmbeddedCoreContainerConfiguration {
    private Path home;
    private boolean deleteOnShutdown = false;

    public Path getHome() {
        return home;
    }

    public void setHome(Path home) {
        this.home = home;
    }

    public boolean isDeleteOnShutdown() {
        return deleteOnShutdown;
    }

    public void setDeleteOnShutdown(boolean deleteOnShutdown) {
        this.deleteOnShutdown = deleteOnShutdown;
    }
}
