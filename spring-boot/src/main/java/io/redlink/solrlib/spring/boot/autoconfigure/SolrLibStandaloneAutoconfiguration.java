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
import io.redlink.solrlib.standalone.SolrServerConnector;
import io.redlink.solrlib.standalone.SolrServerConnectorConfiguration;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;

import java.util.Collections;
import java.util.Set;

@Configuration
@ConditionalOnClass({SolrCoreContainer.class, SolrServerConnector.class})
@EnableConfigurationProperties(SolrLibProperties.class)
@AutoConfigureBefore({SolrLibEmbeddedAutoconfiguration.class})
public class SolrLibStandaloneAutoconfiguration {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final SolrLibProperties props;
    private final Set<SolrCoreDescriptor> coreDescriptors;

    public SolrLibStandaloneAutoconfiguration(SolrLibProperties props, ObjectProvider<Set<SolrCoreDescriptor>> coreDescriptors) {
        this.props = props;
        this.coreDescriptors = ObjectUtils.defaultIfNull(coreDescriptors.getIfAvailable(), Collections.emptySet());
    }

    @Primary
    @Bean(initMethod = "initialize", destroyMethod = "shutdown")
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    @ConditionalOnClass(SolrServerConnector.class)
    @ConditionalOnProperty({"solrlib.base-url"})
    @ConditionalOnMissingBean(SolrCoreContainer.class)
    public SolrCoreContainer solrServerConnector() {
        log.debug("Creating solrServerConnector");
        final SolrServerConnectorConfiguration config = new SolrServerConnectorConfiguration();
        config.setSolrUrl(props.getBaseUrl());
        config.setSolrHome(props.getHome());
        config.setPrefix(props.getCollectionPrefix());
        config.setDeployCores(props.isDeployCores());

        return new SolrServerConnector(coreDescriptors, config);
    }

}