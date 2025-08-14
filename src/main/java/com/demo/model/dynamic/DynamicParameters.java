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
@Table(name = "dynamic_parameters")
@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class DynamicParameters {
    @EmbeddedId
    private DynamicParametersId id;

    @Column(name = "location", length = 10, nullable = false)
    private String location;

    @Column(name = "phaseorder", length = 5, nullable = false)
    private String phaseOrder;

    @Column(name = "cycletime", nullable = false)
    private Integer cycleTime;

    @Column(name = "`offset`", nullable = false)
    private Integer offset;

    @Column(name = "green", nullable = false)
    private Integer green;

    @Column(name = "pedgreen_flash", nullable = false)
    private Integer pedGreenFlash;

    @Column(name = "pedred", nullable = false)
    private Integer pedRed;

    @Column(name = "yellow", nullable = false)
    private Integer yellow;

    @Column(name = "allred", nullable = false)
    private Integer allRed;

    @Column(name = "mingreen", nullable = false)
    private Integer minGreen;

    @Column(name = "maxgreen", nullable = false)
    private Integer maxGreen;

    @Column(name = "direct", nullable = false)
    private Integer direct;

    @Column(name = "log_time", nullable = false, insertable = false, updatable = false,
            columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private LocalDateTime logTime;
}
