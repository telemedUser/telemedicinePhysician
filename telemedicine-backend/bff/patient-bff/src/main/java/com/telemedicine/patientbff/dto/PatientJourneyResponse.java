package com.telemedicine.patientbff.dto;

public record PatientJourneyResponse(
        String appointmentId,
        AggregationStatus status,
        ConsentState consent,
        IdentityState identity,
        WaitingRoomState waitingRoom,
        VideoState video,
        DocumentsState documents,
        SurveyState survey) {}
