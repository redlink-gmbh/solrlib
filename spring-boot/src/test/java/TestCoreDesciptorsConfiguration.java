/*
 * Copyright (c) 2017 Redlink GmbH.
 */

import io.redlink.solrlib.SimpleCoreDescriptor;
import io.redlink.solrlib.SolrCoreDescriptor;
import io.redlink.utils.ResourceLoaderUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

/**
 */
@Configuration
public class TestCoreDesciptorsConfiguration {

    public static final String CORE_NAME = "foo";

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public SolrCoreDescriptor foo() {
        return new SimpleCoreDescriptor(CORE_NAME, ResourceLoaderUtils.getResourceAsPath("/basic.zip", TestCoreDesciptorsConfiguration.class));
    }

}
