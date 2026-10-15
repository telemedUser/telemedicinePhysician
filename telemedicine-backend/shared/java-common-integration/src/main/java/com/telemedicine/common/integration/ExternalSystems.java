package com.telemedicine.common.integration;

import java.util.List;

/** Logical names for the eleven Phase 0 external simulators and production integrations. */
public final class ExternalSystems {

    public static final String HIS = "his";
    public static final String SAP = "sap";
    public static final String PAYMENT = "payment";
    public static final String FONASA = "fonasa";
    public static final String MPI = "mpi";
    public static final String ZENDESK = "zendesk";
    public static final String VONAGE = "vonage";
    public static final String ACS = "acs";
    public static final String ANURA = "anura";
    public static final String RECMED = "recmed";
    public static final String MEDIPASS = "medipass";

    public static final List<String> ALL =
            List.of(HIS, SAP, PAYMENT, FONASA, MPI, ZENDESK, VONAGE, ACS, ANURA, RECMED, MEDIPASS);

    private ExternalSystems() {}
}
