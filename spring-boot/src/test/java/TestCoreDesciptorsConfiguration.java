/*
 * Copyright (c) 2017 Redlink GmbH.
 */

import io.redlink.solrlib.SimpleCoreDescriptor;
import io.redlink.solrlib.SolrCoreDescriptor;
import io.redlink.utils.ResourceLoaderUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 */
@Configuration
public class TestCoreDesciptorsConfiguration {

    @Bean
    public SolrCoreDescriptor foo() {
        return new SimpleCoreDescriptor("foo", ResourceLoaderUtils.getResourceAsPath("/basic.zip", TestCoreDesciptorsConfiguration.class));
    }


}
