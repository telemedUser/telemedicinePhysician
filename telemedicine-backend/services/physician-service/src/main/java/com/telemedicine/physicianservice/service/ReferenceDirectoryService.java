package com.telemedicine.physicianservice.service;

import com.telemedicine.physicianservice.api.dto.ReferenceDtos.DiagnosisOption;
import com.telemedicine.physicianservice.api.dto.ReferenceDtos.DiagnosticTestOption;
import com.telemedicine.physicianservice.api.dto.ReferenceDtos.MedicationOption;
import com.telemedicine.physicianservice.domain.reference.DiagnosisCodeRepository;
import com.telemedicine.physicianservice.domain.reference.DiagnosticTestRepository;
import com.telemedicine.physicianservice.domain.reference.MedicationRepository;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serves the D-08 care-plan dropdowns from the DB-backed reference directories. A blank query
 * returns a default (ordered) slice so a freshly-opened dropdown has suggestions.
 */
@Service
public class ReferenceDirectoryService {

    private static final int LIMIT = 20;

    private final DiagnosisCodeRepository diagnosisRepository;
    private final MedicationRepository medicationRepository;
    private final DiagnosticTestRepository diagnosticTestRepository;

    public ReferenceDirectoryService(
            DiagnosisCodeRepository diagnosisRepository,
            MedicationRepository medicationRepository,
            DiagnosticTestRepository diagnosticTestRepository) {
        this.diagnosisRepository = diagnosisRepository;
        this.medicationRepository = medicationRepository;
        this.diagnosticTestRepository = diagnosticTestRepository;
    }

    @Transactional(readOnly = true)
    public List<DiagnosisOption> searchDiagnoses(String q) {
        Pageable page = PageRequest.of(0, LIMIT);
        var rows = isBlank(q)
                ? diagnosisRepository.findByActiveTrueOrderByCode(page)
                : diagnosisRepository.search(q.trim(), page);
        return rows.stream().map(d -> new DiagnosisOption(d.getCode(), d.getTitle(), d.getSystem())).toList();
    }

    @Transactional(readOnly = true)
    public List<MedicationOption> searchMedications(String q) {
        Pageable page = PageRequest.of(0, LIMIT);
        var rows = isBlank(q)
                ? medicationRepository.findByActiveTrueOrderByName(page)
                : medicationRepository.search(q.trim(), page);
        return rows.stream()
                .map(m -> new MedicationOption(m.getId(), m.getName(), m.getStrength(), m.getForm()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DiagnosticTestOption> searchDiagnosticTests(String q) {
        Pageable page = PageRequest.of(0, LIMIT);
        var rows = isBlank(q)
                ? diagnosticTestRepository.findByActiveTrueOrderByName(page)
                : diagnosticTestRepository.search(q.trim(), page);
        return rows.stream()
                .map(t -> new DiagnosticTestOption(t.getId(), t.getName(), t.getCategory(), t.getSpecimen()))
                .toList();
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
