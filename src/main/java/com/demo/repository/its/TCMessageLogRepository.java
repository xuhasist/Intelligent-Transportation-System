package com.demo.repository.its;

import com.demo.model.its.TCMessageLog;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;

public interface TCMessageLogRepository extends JpaRepository<TCMessageLog, Long> {
    Page<TCMessageLog> findByLogTimeBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
}
