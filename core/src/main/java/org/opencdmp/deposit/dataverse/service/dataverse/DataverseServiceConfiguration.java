package org.opencdmp.deposit.dataverse.service.dataverse;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({DataverseServiceProperties.class})
public class DataverseServiceConfiguration {
}
