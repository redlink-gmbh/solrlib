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
