package com.demo.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DynamicStatus {
    SUCCESS("Y"), FAILURE("N");

    private final String code;
}
