package io.redlink.solrlib.spring.boot.autoconfigure;

import io.redlink.solrlib.SolrCoreContainer;
import io.redlink.solrlib.SolrCoreDescriptor;
import io.redlink.solrlib.standalone.SolrServerConnector;
import io.redlink.solrlib.standalone.SolrServerConnectorConfiguration;
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
@ConditionalOnClass({SolrCoreContainer.class, SolrServerConnector.class})
@EnableConfigurationProperties(SolrLibProperties.class)
public class SolrLibStandaloneAutoconfiguration {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final SolrLibProperties props;
    private final Set<SolrCoreDescriptor> coreDescriptors;

    public SolrLibStandaloneAutoconfiguration(SolrLibProperties props, Optional<Set<SolrCoreDescriptor>> coreDescriptors) {
        this.props = props;
        this.coreDescriptors = coreDescriptors.orElseGet(Collections::emptySet);
    }

    @Primary
    @Bean(initMethod = "initialize", destroyMethod = "shutdown")
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    @ConditionalOnClass(SolrServerConnector.class)
    @ConditionalOnProperty({"solrlib.base-url", "solrlib.home"})
    @ConditionalOnMissingBean(SolrCoreContainer.class)
    public SolrCoreContainer solrServerConnector() {
        log.debug("Creating solrServerConnector");
        final SolrServerConnectorConfiguration config = new SolrServerConnectorConfiguration();
        config.setSolrUrl(props.getBaseUrl());
        config.setSolrHome(props.getHome());
        config.setPrefix(props.getCollectionPrefix());

        return new SolrServerConnector(coreDescriptors, config);
    }

}