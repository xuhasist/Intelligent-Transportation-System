package com.demo.dto;

import lombok.*;

@Builder(toBuilder = true)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SystemLogResponse<T> {
    private T data;
}
