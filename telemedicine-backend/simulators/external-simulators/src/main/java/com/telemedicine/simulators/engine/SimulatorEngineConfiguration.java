package com.telemedicine.simulators.engine;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SimulatorProperties.class)
public class SimulatorEngineConfiguration {}
