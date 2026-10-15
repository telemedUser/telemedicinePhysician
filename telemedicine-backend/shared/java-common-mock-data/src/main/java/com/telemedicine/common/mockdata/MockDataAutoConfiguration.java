package com.telemedicine.common.mockdata;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Profile;

/**
 * Optional mock datastore support. Delete this module when all services use Postgres.
 */
@AutoConfiguration
@Profile("mock")
@EnableConfigurationProperties(DatastoreProperties.class)
public class MockDataAutoConfiguration {
}
