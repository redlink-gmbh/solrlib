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
import io.redlink.solrlib.embedded.EmbeddedCoreContainer;
import io.redlink.solrlib.embedded.EmbeddedCoreContainerConfiguration;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;

@Configuration
@ConditionalOnClass({SolrCoreContainer.class, EmbeddedCoreContainer.class})
@EnableConfigurationProperties(SolrLibProperties.class)
@AutoConfigureAfter({SolrLibCloudAutoconfiguration.class, SolrLibStandaloneAutoconfiguration.class})
public class SolrLibEmbeddedAutoconfiguration {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final SolrLibProperties props;
    private final Set<SolrCoreDescriptor> coreDescriptors;

    public SolrLibEmbeddedAutoconfiguration(SolrLibProperties props, ObjectProvider<Set<SolrCoreDescriptor>> coreDescriptors) {
        this.props = props;
        this.coreDescriptors = ObjectUtils.defaultIfNull(coreDescriptors.getIfAvailable(), Collections.emptySet());
    }

    @Bean(initMethod = "initialize", destroyMethod = "shutdown")
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    @ConditionalOnMissingBean(SolrCoreContainer.class)
    public SolrCoreContainer embeddedSolrCoreContainer() {
        log.debug("Creating embeddedSolrCoreContainer");
        final EmbeddedCoreContainerConfiguration config = new EmbeddedCoreContainerConfiguration();

        Path solrHome = props.getHome();
        if (solrHome == null) {
            try {
                solrHome = Files.createTempDirectory("solrlib");
                log.warn("No solrlib.home provided, creating tempdir {}", solrHome);
            } catch (IOException e) {
                throw new IllegalStateException("Could not create temp-dir as solr home", e);
            }
        }
        config.setHome(solrHome);
        config.setDeleteOnShutdown(props.isDeleteOnShutdown());

        return new EmbeddedCoreContainer(coreDescriptors, config);
    }

}