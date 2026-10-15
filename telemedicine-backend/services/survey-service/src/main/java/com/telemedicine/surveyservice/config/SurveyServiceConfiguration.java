package com.telemedicine.surveyservice.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SurveyServiceProperties.class)
public class SurveyServiceConfiguration {}
