/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package io.redlink.solrlib.embedded;

import java.nio.file.Path;

/**
 * Created by jakob on 25.01.17.
 */
public class EmbeddedCoreContainerConfiguration {
    private Path home;

    public Path getHome() {
        return home;
    }

    public void setHome(Path home) {
        this.home = home;
    }
}
