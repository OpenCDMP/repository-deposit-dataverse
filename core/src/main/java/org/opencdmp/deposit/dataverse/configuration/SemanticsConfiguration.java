package org.opencdmp.deposit.dataverse.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({SemanticsProperties.class})
public class SemanticsConfiguration {
}
