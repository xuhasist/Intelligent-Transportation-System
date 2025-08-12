package com.demo.dto;

import lombok.*;

@Builder(toBuilder = true)
@Setter
@Getter
@AllArgsConstructor
public class ConditionDto {
    private final String conditionExpression;
    private final Integer consecutiveMatches; // should match this many times
    private Integer consecutiveCounts;        // record the real count of matches
}
