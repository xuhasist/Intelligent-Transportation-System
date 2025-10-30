package com.demo.dto;

import lombok.*;

import java.util.List;

@Builder(toBuilder = true)
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class ThresholdDto {
    private final List<String> cctvList;
    private final Integer timeInterval;
    private final List<String> carflowDirectionList;
    private final String comparisonOperator;
    private final Integer thresholdValue;
    private Boolean isMatch;
}
