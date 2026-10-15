package com.telemedicine.common.security;

/** How the shared filter chain authenticates callers. */
public enum SecurityMode {

    /** Bearer JWT validated against Keycloak. The only mode permitted in production. */
    JWT,

    /**
     * Every request is permitted and no authentication is performed. For local development and
     * component tests only; the startup guard refuses to let this reach a production profile.
     */
    DEV
}
