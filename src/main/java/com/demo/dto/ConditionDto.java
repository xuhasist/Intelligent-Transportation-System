package com.demo.dto;

import lombok.*;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Builder(toBuilder = true)
@Setter
@Getter
@AllArgsConstructor
public class ConditionDto {
    private final String conditionExpression;
    private final Integer consecutiveMatches;   // should match this many times
    @Builder.Default
    private AtomicInteger consecutiveCounts = new AtomicInteger(0); // record the real count of matches
    @Builder.Default
    private AtomicLong lastTriggeredTime = new AtomicLong(0);
}
