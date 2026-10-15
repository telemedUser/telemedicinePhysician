package com.telemedicine.physicianbff.dto.downstream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ClinicalDocumentResponse(String documentId, String event) {}
