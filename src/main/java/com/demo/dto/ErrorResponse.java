package com.demo.dto;

import lombok.*;
import org.springframework.http.HttpStatus;

@Builder(toBuilder = true)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResponse {
    private HttpStatus httpStatus;
    private String message;
}
