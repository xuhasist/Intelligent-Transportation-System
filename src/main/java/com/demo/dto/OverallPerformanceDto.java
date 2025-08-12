package com.demo.dto;

import lombok.*;

@Builder(toBuilder = true)
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class OverallPerformanceDto {
    private Double totalPerformanceRate;
}
