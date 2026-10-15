package com.telemedicine.monitoringservice.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MonitoringServiceProperties.class)
public class MonitoringServiceConfiguration {}
