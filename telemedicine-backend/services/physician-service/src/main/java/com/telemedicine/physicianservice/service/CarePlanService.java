package com.telemedicine.physicianservice.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemedicine.physicianservice.api.dto.CarePlanDto;
import com.telemedicine.physicianservice.api.dto.CarePlanDto.ClinicalNote;
import com.telemedicine.physicianservice.api.dto.CarePlanDto.Diagnosis;
import com.telemedicine.physicianservice.api.dto.CarePlanDto.DiagnosticTest;
import com.telemedicine.physicianservice.api.dto.CarePlanDto.FollowUp;
import com.telemedicine.physicianservice.api.dto.CarePlanDto.MedicalLeave;
import com.telemedicine.physicianservice.api.dto.CarePlanDto.Prescription;
import com.telemedicine.physicianservice.api.dto.CarePlanDto.Summary;
import com.telemedicine.physicianservice.domain.careplan.CarePlanEntity;
import com.telemedicine.physicianservice.domain.careplan.CarePlanRepository;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads and saves the D-08 care plan. Lists are (de)serialized to/from the entity's JSON text
 * columns. The physician app sends the whole plan on each save (edit→done is client-side).
 */
@Service
public class CarePlanService {

    private static final Logger log = LoggerFactory.getLogger(CarePlanService.class);
    private static final TypeReference<List<Diagnosis>> DIAGNOSES = new TypeReference<>() {};
    private static final TypeReference<List<Prescription>> PRESCRIPTIONS = new TypeReference<>() {};
    private static final TypeReference<List<DiagnosticTest>> TESTS = new TypeReference<>() {};

    private final CarePlanRepository repository;
    private final ObjectMapper objectMapper;

    public CarePlanService(CarePlanRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    /** Load the plan, or an empty default if none exists yet. */
    @Transactional(readOnly = true)
    public CarePlanDto get(String appointmentId) {
        CarePlanEntity e = repository.findById(appointmentId).orElse(null);
        if (e == null) {
            return empty(appointmentId);
        }
        return toDto(e);
    }

    /** Upsert the plan for the signed-in physician. */
    @Transactional
    public CarePlanDto save(String appointmentId, String physicianId, String market, CarePlanDto dto) {
        CarePlanEntity e =
                repository.findById(appointmentId).orElseGet(() -> new CarePlanEntity(appointmentId, market));
        if (physicianId != null) {
            e.setProviderId(physicianId);
        }
        if (market != null) {
            e.setMarket(market);
        }
        ClinicalNote note = dto.note() != null ? dto.note() : new ClinicalNote(null, null, null, null);
        e.setNoteHistory(note.history());
        e.setNoteFindings(note.findings());
        e.setNoteAssessment(note.assessment());
        e.setNotePlan(note.plan());

        e.setDiagnoses(write(dto.diagnoses()));
        e.setPrescriptions(write(dto.prescriptions()));
        e.setDiagnosticTests(write(dto.diagnosticTests()));

        FollowUp f = dto.followUp();
        e.setFollowUpEnabled(f != null && f.enabled());
        e.setFollowUpMode(f != null ? f.mode() : null);
        e.setFollowUpInDays(f != null ? f.inDays() : null);
        e.setFollowUpDate(f != null ? f.date() : null);
        e.setFollowUpInstructions(f != null ? f.instructions() : null);

        MedicalLeave l = dto.medicalLeave();
        e.setLeaveEnabled(l != null && l.enabled());
        e.setLeaveReason(l != null ? l.reason() : null);
        e.setLeaveStartDate(l != null ? l.startDate() : null);
        e.setLeaveEndDate(l != null ? l.endDate() : null);

        Summary s = dto.summary();
        e.setSummaryText(s != null ? s.text() : null);
        e.setSummaryInstructions(s != null ? s.instructions() : null);

        if (dto.status() != null && !dto.status().isBlank()) {
            e.setStatus(dto.status());
        }
        e.setUpdatedAt(Instant.now());
        repository.save(e);
        log.debug("Care plan saved for {}", appointmentId);
        return toDto(e);
    }

    private CarePlanDto toDto(CarePlanEntity e) {
        return new CarePlanDto(
                e.getAppointmentId(),
                new ClinicalNote(e.getNoteHistory(), e.getNoteFindings(), e.getNoteAssessment(), e.getNotePlan()),
                read(e.getDiagnoses(), DIAGNOSES),
                read(e.getPrescriptions(), PRESCRIPTIONS),
                read(e.getDiagnosticTests(), TESTS),
                new FollowUp(
                        e.isFollowUpEnabled(),
                        e.getFollowUpMode(),
                        e.getFollowUpInDays(),
                        e.getFollowUpDate(),
                        e.getFollowUpInstructions()),
                new MedicalLeave(
                        e.isLeaveEnabled(), e.getLeaveReason(), e.getLeaveStartDate(), e.getLeaveEndDate()),
                new Summary(e.getSummaryText(), e.getSummaryInstructions()),
                e.getStatus());
    }

    private static CarePlanDto empty(String appointmentId) {
        return new CarePlanDto(
                appointmentId,
                new ClinicalNote(null, null, null, null),
                List.of(),
                List.of(),
                List.of(),
                new FollowUp(false, null, null, null, null),
                new MedicalLeave(false, null, null, null),
                new Summary(null, null),
                "DRAFT");
    }

    private String write(List<?> list) {
        try {
            return objectMapper.writeValueAsString(list == null ? List.of() : list);
        } catch (Exception ex) {
            log.warn("Could not serialize care-plan list: {}", ex.getMessage());
            return "[]";
        }
    }

    private <T> List<T> read(String json, TypeReference<List<T>> type) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception ex) {
            log.debug("Could not parse care-plan list: {}", ex.getMessage());
            return List.of();
        }
    }
}
