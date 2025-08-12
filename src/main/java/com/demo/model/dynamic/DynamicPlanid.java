package com.demo.model.dynamic;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "dynamic_planid")
@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class DynamicPlanid {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "program_id", nullable = false, length = 10)
    private String programId;

    @Column(name = "tc_id", nullable = false, length = 10)
    private String tcId;

    @Column(name = "day", nullable = false, length = 5)
    private String day;

    @Column(name = "time", nullable = false, length = 10)
    private String time;

    @Column(name = "plan_id", nullable = false)
    private Integer planId;

    @Column(name = "log_time", nullable = false)
    private LocalDateTime logTime;
}
