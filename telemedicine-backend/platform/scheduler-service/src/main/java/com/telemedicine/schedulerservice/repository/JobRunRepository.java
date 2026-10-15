package com.telemedicine.schedulerservice.repository;

import com.telemedicine.schedulerservice.domain.JobRun;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobRunRepository extends JpaRepository<JobRun, String> {

    Page<JobRun> findByJobKey(String jobKey, Pageable pageable);

    Page<JobRun> findAllByOrderByStartedAtDesc(Pageable pageable);
}
