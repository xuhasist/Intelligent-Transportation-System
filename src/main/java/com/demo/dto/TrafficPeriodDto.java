package com.demo.dto;

import lombok.*;

import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicBoolean;

@Builder(toBuilder = true)
@Setter
@Getter
@AllArgsConstructor
public class TrafficPeriodDto {
    private final String programId;
    private final Integer subId;
    private final LocalTime startTime;
    private final LocalTime endTime;
    @Builder.Default
    private AtomicBoolean inSchedule = new AtomicBoolean(false);
}
