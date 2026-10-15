package com.telemedicine.clinicaldocsservice.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "telemedicine.clinical-docs")
public class ClinicalDocsServiceProperties {

    private String clinicalDocumentsBucket = "clinical-documents";
    private String prescriptionsBucket = "prescriptions";
    private Duration downloadTokenTtl = Duration.ofHours(1);
    private String publicBaseUrl = "http://localhost:8105";

    public String getClinicalDocumentsBucket() {
        return clinicalDocumentsBucket;
    }

    public void setClinicalDocumentsBucket(String clinicalDocumentsBucket) {
        this.clinicalDocumentsBucket = clinicalDocumentsBucket;
    }

    public String getPrescriptionsBucket() {
        return prescriptionsBucket;
    }

    public void setPrescriptionsBucket(String prescriptionsBucket) {
        this.prescriptionsBucket = prescriptionsBucket;
    }

    public Duration getDownloadTokenTtl() {
        return downloadTokenTtl;
    }

    public void setDownloadTokenTtl(Duration downloadTokenTtl) {
        this.downloadTokenTtl = downloadTokenTtl;
    }

    public String getPublicBaseUrl() {
        return publicBaseUrl;
    }

    public void setPublicBaseUrl(String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }
}
