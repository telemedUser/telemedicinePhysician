package com.telemedicine.schedulerservice.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RestClientConfig.SchedulerProperties.class)
public class SchedulerPropertiesConfig {}
