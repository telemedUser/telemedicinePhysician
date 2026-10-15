package com.telemedicine.integrationservice.service;

import com.telemedicine.integrationservice.adapter.FonasaAdapter;
import com.telemedicine.integrationservice.adapter.HisAdapter;
import com.telemedicine.integrationservice.adapter.ZendeskAdapter;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("postgres")
public class InternalIntegrationService {

    private final FonasaAdapter fonasaAdapter;
    private final HisAdapter hisAdapter;
    private final ZendeskAdapter zendeskAdapter;

    public InternalIntegrationService(
            FonasaAdapter fonasaAdapter, HisAdapter hisAdapter, ZendeskAdapter zendeskAdapter) {
        this.fonasaAdapter = fonasaAdapter;
        this.hisAdapter = hisAdapter;
        this.zendeskAdapter = zendeskAdapter;
    }

    public FonasaAdapter.ChallengeResult fonasaChallenge(String patientId) {
        return fonasaAdapter.challenge(patientId);
    }

    public FonasaAdapter.VerifyResult fonasaVerify(String patientId, String challengeId, String code) {
        return fonasaAdapter.verify(patientId, challengeId, code);
    }

    public HisAdapter.HisStatusUpdate updateHisStatus(String appointmentId, String status) {
        return hisAdapter.updateStatus(appointmentId, status);
    }

    public ZendeskAdapter.TicketResult createTicket(Map<String, String> body) {
        return zendeskAdapter.createTicket(
                body.getOrDefault("subject", "support"),
                body.getOrDefault("description", ""),
                body.getOrDefault("priority", "low"));
    }
}
