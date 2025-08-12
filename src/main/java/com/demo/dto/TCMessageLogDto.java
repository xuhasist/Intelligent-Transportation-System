package com.demo.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Builder(toBuilder = true)
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TCMessageLogDto {
    private Long id;
    private String deviceId;
    private String messageId;
    private Map<String, Object> jsonValue;
    private String rawValue;
    private Integer noteCode;
    private String returnResult;
    private LocalDateTime logTime;
}
