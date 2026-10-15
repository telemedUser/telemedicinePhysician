package com.telemedicine.common.integration.acl;

/**
 * Translates between domain models and external system payloads at the anti-corruption layer.
 *
 * @param <REQ> domain request type
 * @param <EXT_REQ> wire request type accepted by the external system
 * @param <EXT_RES> wire response type returned by the external system
 * @param <RES> domain response type
 */
public interface AntiCorruptionMapper<REQ, EXT_REQ, EXT_RES, RES> {

    EXT_REQ toExternal(REQ domainRequest);

    RES toDomain(EXT_RES externalResponse);
}
