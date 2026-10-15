package com.telemedicine.clinicaldocsservice.repository;

import com.telemedicine.clinicaldocsservice.domain.DownloadTokenEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DownloadTokenRepository extends JpaRepository<DownloadTokenEntity, String> {
    Optional<DownloadTokenEntity> findByTokenHash(String tokenHash);
}
