package com.telemedicine.integrationservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.telemedicine.common.core.error.DomainException;
import com.telemedicine.common.core.error.ErrorCode;
import com.telemedicine.common.events.payload.AppointmentCreated;
import com.telemedicine.common.events.payload.PaymentConfirmed;
import com.telemedicine.common.integration.TelemedicineIntegrationProperties;
import com.telemedicine.common.integration.acl.IntegrationFailureReporter;
import com.telemedicine.common.integration.client.IntegrationHttpClientFactory;
import com.telemedicine.common.persistence.outbox.OutboxRecorder;
import com.telemedicine.common.testing.wiremock.SimulatorStubs;
import com.telemedicine.integrationservice.adapter.HisAdapter;
import com.telemedicine.integrationservice.adapter.MpiAdapter;
import com.telemedicine.integrationservice.adapter.PaymentAdapter;
import com.telemedicine.integrationservice.domain.Appointment;
import com.telemedicine.integrationservice.domain.AppointmentRepository;
import com.telemedicine.integrationservice.domain.AppointmentStatus;
import com.telemedicine.integrationservice.service.PostgresAppointmentService;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AppointmentServicePostgresTest {

    private WireMockServer wireMockServer;

    @Mock private AppointmentRepository appointmentRepository;
    @Mock private OutboxRecorder outboxRecorder;

    private PostgresAppointmentService appointmentService;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(0);
        wireMockServer.start();
        SimulatorStubs.registerAll(wireMockServer);

        TelemedicineIntegrationProperties properties = new TelemedicineIntegrationProperties();
        properties.getSimulator().setBaseUrl(wireMockServer.baseUrl());
        properties.getHttp().setLogRequests(false);

        IntegrationHttpClientFactory clientFactory =
                new IntegrationHttpClientFactory(properties, java.util.List.of(), java.util.List.of(), null);
        IntegrationFailureReporter failureReporter =
                new IntegrationFailureReporter(Optional.empty());

        HisAdapter hisAdapter = new HisAdapter(clientFactory, failureReporter, properties);
        PaymentAdapter paymentAdapter = new PaymentAdapter(clientFactory, failureReporter, properties);
        MpiAdapter mpiAdapter = new MpiAdapter(clientFactory, failureReporter, properties);

        appointmentService =
                new PostgresAppointmentService(
                        appointmentRepository, outboxRecorder, hisAdapter, paymentAdapter, mpiAdapter);
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void createPersistsAndRecordsAppointmentCreated() {
        when(appointmentRepository.save(any(Appointment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Appointment created = appointmentService.create(Map.of("patientId", "pat-1", "physicianId", "phy-1"));

        assertThat(created.id()).startsWith("apt-");
        assertThat(created.status()).isEqualTo(AppointmentStatus.CREATED);

        ArgumentCaptor<AppointmentCreated> eventCaptor = ArgumentCaptor.forClass(AppointmentCreated.class);
        verify(outboxRecorder)
                .record(eq("Appointment"), eq(created.id()), eventCaptor.capture(), any(), eq(created.id()));
        assertThat(eventCaptor.getValue().appointmentId()).isEqualTo(created.id());
    }

    @Test
    void validatePaymentRequiresAdapterConfirmation() {
        Appointment pending =
                new Appointment(
                        "apt-unpaid",
                        "pat-1",
                        "phy-1",
                        AppointmentStatus.SCHEDULED,
                        java.time.Instant.now().toString(),
                        "PENDING",
                        "his");
        when(appointmentRepository.findById("apt-unpaid")).thenReturn(Optional.of(pending));

        wireMockServer.stubFor(
                com.github.tomakehurst.wiremock.client.WireMock.get(
                                com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo(
                                        "/sim/payment/validate/apt-unpaid"))
                        .willReturn(
                                com.github.tomakehurst.wiremock.client.WireMock.aResponse()
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(
                                                """
                                                {"appointmentId":"apt-unpaid","paid":false,"transactionId":"txn-0"}
                                                """)));

        assertThatThrownBy(() -> appointmentService.validatePayment("apt-unpaid"))
                .isInstanceOf(DomainException.class)
                .extracting(ex -> ((DomainException) ex).getCode())
                .isEqualTo(ErrorCode.PAYMENT_NOT_CONFIRMED);
    }

    @Test
    void validatePaymentRecordsPaymentConfirmedWhenPaid() {
        Appointment pending =
                new Appointment(
                        "apt-001",
                        "pat-1",
                        "phy-1",
                        AppointmentStatus.SCHEDULED,
                        java.time.Instant.now().toString(),
                        "PENDING",
                        "his");
        when(appointmentRepository.findById("apt-001")).thenReturn(Optional.of(pending));
        when(appointmentRepository.save(any(Appointment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Appointment confirmed = appointmentService.validatePayment("apt-001");

        assertThat(confirmed.paymentStatus()).isEqualTo("CONFIRMED");
        verify(outboxRecorder)
                .record(eq("Appointment"), eq("apt-001"), any(PaymentConfirmed.class), any(), eq("apt-001"));
    }
}
