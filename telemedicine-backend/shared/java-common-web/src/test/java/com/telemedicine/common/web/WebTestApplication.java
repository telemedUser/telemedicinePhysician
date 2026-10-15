package com.telemedicine.common.web;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

/**
 * Minimal host application for the slice tests.
 *
 * <p>Deliberately carries no {@code @ComponentScan}: the whole point of these tests is to prove
 * that {@code WebAutoConfiguration} registers the shared beans from the jar's {@code
 * AutoConfiguration.imports} without the application scanning {@code com.telemedicine.common},
 * which is exactly the situation in the seventeen services.
 */
@SpringBootConfiguration
@EnableAutoConfiguration
public class WebTestApplication {}
