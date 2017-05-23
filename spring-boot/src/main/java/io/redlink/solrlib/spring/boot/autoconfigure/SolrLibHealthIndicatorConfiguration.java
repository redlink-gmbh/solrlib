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
package io.redlink.solrlib.spring.boot.autoconfigure;

import io.redlink.solrlib.SolrCoreContainer;
import io.redlink.solrlib.SolrCoreDescriptor;
import org.apache.solr.client.solrj.SolrClient;
import org.springframework.boot.actuate.autoconfigure.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.health.*;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

/**
 */
@Configuration
@AutoConfigureAfter({SolrLibEmbeddedAutoconfiguration.class, SolrLibStandaloneAutoconfiguration.class, SolrLibCloudAutoconfiguration.class})
@ConditionalOnBean(SolrCoreContainer.class)
@ConditionalOnEnabledHealthIndicator("solrlib")
public class SolrLibHealthIndicatorConfiguration {

    private final SolrCoreContainer solrCoreContainer;
    private final Set<SolrCoreDescriptor> coreDescriptors;

    private final HealthAggregator healthAggregator;

    public SolrLibHealthIndicatorConfiguration(SolrCoreContainer solrCoreContainer, Set<SolrCoreDescriptor> coreDescriptors, HealthAggregator healthAggregator) {
        this.solrCoreContainer = solrCoreContainer;
        this.coreDescriptors = coreDescriptors;
        this.healthAggregator = healthAggregator;
    }

    @Bean
    @ConditionalOnMissingBean(name = "solrlib")
    public HealthIndicator solrlibHealthIndicator() {
        final CompositeHealthIndicator healthIndicator = new CompositeHealthIndicator(healthAggregator);

        healthIndicator.addHealthIndicator("coreContainer", new CoreContainerHealthIndicator(solrCoreContainer));

        final CompositeHealthIndicator coreIndicator = new CompositeHealthIndicator(healthAggregator);
        for (SolrCoreDescriptor descriptor : coreDescriptors) {
            coreIndicator.addHealthIndicator(descriptor.getCoreName(),
                    new CoreDescriptorHealthIndicator(solrCoreContainer, descriptor));
        }
        healthIndicator.addHealthIndicator("collections", coreIndicator);

        return healthIndicator;
    }

    public static class CoreContainerHealthIndicator extends AbstractHealthIndicator {
        private final SolrCoreContainer solrCoreContainer;

        private CoreContainerHealthIndicator(SolrCoreContainer solrCoreContainer) {
            this.solrCoreContainer = solrCoreContainer;
        }

        @Override
        protected void doHealthCheck(Health.Builder builder) throws Exception {
            if (solrCoreContainer == null) {
                builder.unknown();
                return;
            } else if (!solrCoreContainer.isStartupComplete()) {
                builder.outOfService()
                        .withDetail("startup", "in-progress");
            } else {
                builder.up()
                        .withDetail("startup", "complete");
            }
            builder.withDetail("class", solrCoreContainer.getClass().getName());
        }
    }

    public static class CoreDescriptorHealthIndicator extends AbstractHealthIndicator {
        private final SolrCoreContainer solrCoreContainer;
        private final SolrCoreDescriptor coreDescriptor;

        public CoreDescriptorHealthIndicator(SolrCoreContainer solrCoreContainer, SolrCoreDescriptor coreDescriptor) {
            this.solrCoreContainer = solrCoreContainer;
            this.coreDescriptor = coreDescriptor;
        }

        @Override
        protected void doHealthCheck(Health.Builder builder) throws Exception {
            if (solrCoreContainer == null) {
                builder.unknown();
            } else if (!solrCoreContainer.isStartupComplete()) {
                builder.outOfService();
            } else {
                try (SolrClient solrClient = solrCoreContainer.getSolrClient(coreDescriptor)) {
                    builder.up().withDetail("status", solrClient.ping().getResponse().get("status"));
                }
            }
        }
    }
}
