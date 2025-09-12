package com.demo.model.dynamic;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "dynamic_csv_file")
@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class DynamicCsvFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private Integer version;

    @Column(length = 5, nullable = false)
    private String action;

    @Column(name = "scenario_id", nullable = false)
    private Integer scenarioId;

    @Column(name = "rule_id")
    private Integer ruleId;

    @Column(length = 50)
    private String type;

    @Column(length = 5)
    private String operator;

    @Column(length = 10)
    private String threshold;

    @Column(name = "intersection_id", length = 10)
    private String intersectionId;

    @Column(name = "path_id", length = 10)
    private String pathId;

    @Column(name = "phase_order", length = 10)
    private String phaseOrder;

    @Column(name = "sub_phase_id", length = 50)
    private String subPhaseId;

    @Column(length = 800)
    private String statement;

    @Column(name = "logtime", nullable = false, updatable = false, insertable = false,
            columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime logtime;
}
