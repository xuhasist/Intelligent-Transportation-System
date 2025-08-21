package com.demo.repository.its;

import com.demo.model.its.TcMessageLog;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;

public interface TcMessageLogRepository extends JpaRepository<TcMessageLog, Long> {
    Page<TcMessageLog> findByLogTimeBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
}
