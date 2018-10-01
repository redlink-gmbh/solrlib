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

package io.redlink.solrlib.standalone;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.UUID;

/**
 */
public class SolrServerConnectorTest {

    @Test
    public void testCreateRemoteName() {
        String[] coreNames = {"core1", "collection", "foo"};

        SolrServerConnectorConfiguration configPlain = new SolrServerConnectorConfiguration();
        SolrServerConnector solrServerConnector1 = new SolrServerConnector(Collections.emptySet(), configPlain);
        for (String coreName : coreNames) {
            Assert.assertThat(solrServerConnector1.createRemoteName(coreName), Matchers.is(coreName));
        }

        final String prefix = UUID.randomUUID().toString() + "_";
        SolrServerConnectorConfiguration configPrefix = new SolrServerConnectorConfiguration();
        configPrefix.setPrefix(prefix);
        SolrServerConnector solrServerConnector2 = new SolrServerConnector(Collections.emptySet(), configPrefix);
        for (String coreName : coreNames) {
            Assert.assertThat(solrServerConnector2.createRemoteName(coreName), Matchers.is(prefix + coreName));
        }
    }
}