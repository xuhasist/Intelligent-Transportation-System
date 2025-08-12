package com.demo.model.dynamic;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "dynamic_threshold")
@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class DynamicThreshold {
    @EmbeddedId
    private DynamicThresholdId id;

    @Column(name = "time_label", nullable = false)
    private String timeLabel;

    @Column(name = "cctv_id", nullable = false)
    private String cctvId;

    @Column(name = "time_interval", nullable = false)
    private Integer timeInterval;

    @Column(name = "carflow_direction", nullable = false)
    private String carflowDirection;

    @Column(name = "comparison_operator", nullable = false)
    private String comparisonOperator;

    @Column(name = "threshold_value", nullable = false)
    private Integer thresholdValue;

    @Column(name = "log_time", nullable = false, insertable = false, updatable = false,
            columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private LocalDateTime logTime;
}
