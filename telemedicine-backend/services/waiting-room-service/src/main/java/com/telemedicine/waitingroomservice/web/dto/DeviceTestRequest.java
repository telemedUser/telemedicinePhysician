package com.telemedicine.waitingroomservice.web.dto;

public record DeviceTestRequest(Boolean camera, Boolean microphone, Integer networkMbps) {}
