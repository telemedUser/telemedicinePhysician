package com.telemedicine.waitingroomservice.sessionlink;

import com.telemedicine.common.core.id.IdGenerator;
import com.telemedicine.common.events.payload.LinkGenerated;
import com.telemedicine.common.persistence.outbox.OutboxRecorder;
import com.telemedicine.waitingroomservice.config.WaitingRoomServiceProperties;
import com.telemedicine.waitingroomservice.domain.SessionLinkEntity;
import com.telemedicine.waitingroomservice.integration.IntegrationGateway;
import com.telemedicine.waitingroomservice.repository.SessionLinkRepository;
import com.telemedicine.waitingroomservice.web.dto.SessionLinkRequest;
import com.telemedicine.waitingroomservice.web.dto.SessionLinkResponse;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionLinkService {

    private final SessionLinkRepository sessionLinkRepository;
    private final SessionLinkTokenService tokenService;
    private final IntegrationGateway integrationGateway;
    private final OutboxRecorder outboxRecorder;
    private final WaitingRoomServiceProperties properties;

    public SessionLinkService(
            SessionLinkRepository sessionLinkRepository,
            SessionLinkTokenService tokenService,
            IntegrationGateway integrationGateway,
            OutboxRecorder outboxRecorder,
            WaitingRoomServiceProperties properties) {
        this.sessionLinkRepository = sessionLinkRepository;
        this.tokenService = tokenService;
        this.integrationGateway = integrationGateway;
        this.outboxRecorder = outboxRecorder;
        this.properties = properties;
    }

    @Transactional
    public SessionLinkResponse create(SessionLinkRequest request) {
        integrationGateway.requirePaymentConfirmed(request.appointmentId());

        SessionLinkTokenService.IssuedToken issued = tokenService.issue(request.appointmentId());
        Instant now = Instant.now();

        SessionLinkEntity entity = new SessionLinkEntity();
        entity.setId(issued.linkId());
        entity.setAppointmentId(request.appointmentId());
        entity.setTokenHash(issued.tokenHash());
        entity.setChannel(request.channel() == null ? "SMS" : request.channel());
        entity.setExpiresAt(issued.expiresAt());
        entity.setCreatedAt(now);
        sessionLinkRepository.save(entity);

        LinkGenerated payload =
                new LinkGenerated(
                        request.appointmentId(),
                        issued.linkId(),
                        issued.expiresAt(),
                        entity.getChannel());
        outboxRecorder.record(
                "session-link",
                request.appointmentId(),
                payload,
                IdGenerator.correlationId(),
                request.appointmentId());

        String url = properties.getSessionLinkBaseUrl() + "/" + issued.token();
        return new SessionLinkResponse(
                request.appointmentId(), issued.token(), url, "LinkGenerated");
    }
}
