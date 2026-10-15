package com.telemedicine.integrationservice.service;

import com.telemedicine.integrationservice.domain.Appointment;
import com.telemedicine.integrationservice.domain.MpiPatient;
import java.util.Map;

public interface AppointmentService {
    Appointment create(Map<String, Object> body);

    Appointment consult(String id);

    Appointment updateStatus(String id, String status);

    Appointment reschedule(String id, String newSlot);

    Appointment validatePayment(String id);

    MpiPatient mpiLookup(String appointmentId);
}
