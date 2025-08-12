package com.demo.model.dynamic;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "cctv_carflow_instant")
@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class CctvCarflowInstant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "cctv_id", nullable = false, length = 10)
    private String cctvId;

    @Column(name = "start_position", nullable = false, length = 5)
    private String startPosition;

    @Column(name = "end_position", nullable = false, length = 5)
    private String endPosition;

    @Column(name = "motor", nullable = false)
    private Double motor;

    @Column(name = "car", nullable = false)
    private Double car;

    @Column(name = "truck", nullable = false)
    private Double truck;

    @Column(name = "logtime", nullable = false)
    private LocalDateTime logtime;
}
