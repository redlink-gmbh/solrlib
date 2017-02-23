package io.redlink.solrlib.spring.boot.autoconfigure;

import io.redlink.solrlib.SolrCoreContainer;
import io.redlink.solrlib.SolrCoreDescriptor;
import io.redlink.solrlib.cloud.SolrCloudConnector;
import io.redlink.solrlib.cloud.SolrCloudConnectorConfiguration;
import io.redlink.solrlib.embedded.EmbeddedCoreContainer;
import io.redlink.solrlib.embedded.EmbeddedCoreContainerConfiguration;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

@Configuration
@ConditionalOnClass(SolrCoreContainer.class)
@EnableConfigurationProperties(SolrLibProperties.class)
public class SolrLibAutoconfiguration {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final SolrLibProperties props;
    private final Set<SolrCoreDescriptor> coreDescriptors;

    public SolrLibAutoconfiguration(SolrLibProperties props, Optional<Set<SolrCoreDescriptor>> coreDescriptors) {
        this.props = props;
        this.coreDescriptors = coreDescriptors.orElseGet(Collections::emptySet);
    }


    @Primary
    @Bean(initMethod = "initialize", destroyMethod = "shutdown")
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    @ConditionalOnClass(SolrCloudConnector.class)
    @ConditionalOnProperty("solrlib.zk-connection")
    @ConditionalOnMissingBean(SolrCoreContainer.class)
    public SolrCoreContainer solrCloudConnector() {
        log.debug("Creating solrCloudConnector");
        final SolrCloudConnectorConfiguration config = new SolrCloudConnectorConfiguration();
        config.setZkConnection(props.getZkConnection());
        config.setMaxShardsPerNode(props.getMaxShardsPerNode());
        config.setPrefix(props.getCollectionPrefix());

        return new SolrCloudConnector(coreDescriptors, config);
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

    @Bean(initMethod = "initialize", destroyMethod = "shutdown")
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    @ConditionalOnClass(EmbeddedCoreContainer.class)
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

        return new EmbeddedCoreContainer(coreDescriptors, config);
    }

}