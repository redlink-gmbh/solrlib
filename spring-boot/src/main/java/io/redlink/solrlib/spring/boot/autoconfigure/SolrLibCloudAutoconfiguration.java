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
import io.redlink.solrlib.cloud.SolrCloudConnector;
import io.redlink.solrlib.cloud.SolrCloudConnectorConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

@Configuration
@ConditionalOnClass({SolrCoreContainer.class, SolrCloudConnector.class})
@EnableConfigurationProperties(SolrLibProperties.class)
public class SolrLibCloudAutoconfiguration {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final SolrLibProperties props;
    private final Set<SolrCoreDescriptor> coreDescriptors;

    public SolrLibCloudAutoconfiguration(SolrLibProperties props, Optional<Set<SolrCoreDescriptor>> coreDescriptors) {
        this.props = props;
        this.coreDescriptors = coreDescriptors.orElseGet(Collections::emptySet);
    }


    @Primary
    @Bean(initMethod = "initialize", destroyMethod = "shutdown")
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    @ConditionalOnProperty("solrlib.zk-connection")
    @ConditionalOnMissingBean(SolrCoreContainer.class)
    public SolrCoreContainer solrCloudConnector() {
        log.debug("Creating solrCloudConnector");
        final SolrCloudConnectorConfiguration config = new SolrCloudConnectorConfiguration();
        config.setZkConnection(props.getZkConnection());
        config.setMaxShardsPerNode(props.getMaxShardsPerNode());
        config.setPrefix(props.getCollectionPrefix());
        config.setDeployCores(props.isDeployCores());

        return new SolrCloudConnector(coreDescriptors, config);
    }

}