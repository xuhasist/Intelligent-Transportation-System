package com.demo.dto;

import lombok.*;

@Builder(toBuilder = true)
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ScenarioRuleDto {
    private String scenarioId;
    private String ruleId;
    private String type;
    private String operator;
    private String threshold;
    private String intersectionId;
    private String pathId;
    private String phaseOrder;
    private String subPhaseId;
    private String statement;
    private String action;
    private String version;
}