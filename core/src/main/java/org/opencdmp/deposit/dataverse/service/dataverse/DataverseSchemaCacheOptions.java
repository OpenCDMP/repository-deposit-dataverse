package org.opencdmp.deposit.dataverse.service.dataverse;

import gr.cite.tools.cache.CacheOptions;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "cache.dataverse-citation-schema")
public class DataverseSchemaCacheOptions extends CacheOptions {
}
