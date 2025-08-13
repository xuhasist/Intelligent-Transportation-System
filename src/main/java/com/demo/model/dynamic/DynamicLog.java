package com.demo.model.dynamic;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "dynamic_log")
@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class DynamicLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "program_id", length = 10, nullable = false)
    private String programId;

    @Column(name = "tc_id", length = 10, nullable = false)
    private String tcId;

    @Column(name = "plan_id", nullable = false)
    private Integer planId;

    @Column(name = "status", length = 1, nullable = false)
    private String status;

    @Column(name = "message", length = 255, nullable = false)
    private String message;

    @Column(name = "log_time", nullable = false, insertable = false, updatable = false,
            columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime logTime;
}
