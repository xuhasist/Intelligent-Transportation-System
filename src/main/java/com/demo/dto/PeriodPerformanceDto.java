package com.demo.dto;

import lombok.*;

@Builder(toBuilder = true)
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class PeriodPerformanceDto {
    private String holidayLabel;  // weekday or weekend
    private String periodLabel;   // time period label
    private Double avgBeforeCount;
    private Double avgAfterCount;
    private Double segmentPerformanceRate;
}
