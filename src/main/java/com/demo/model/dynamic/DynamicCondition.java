package com.demo.model.dynamic;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "dynamic_condition")
@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class DynamicCondition {
    @Id
    @Column(name = "program_id", length = 10, nullable = false)
    private String programId;

    @Column(name = "condition_expression", length = 150, nullable = false)
    private String conditionExpression;

    @Column(name = "consecutive_matches", nullable = false)
    private Integer consecutiveMatches;

    @CreationTimestamp
    @Column(name = "log_time", nullable = false, updatable = false, insertable = false)
    private LocalDateTime logTime;
}
