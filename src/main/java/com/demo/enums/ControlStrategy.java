package com.demo.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ControlStrategy {
    TOD(5), Dynamic(6);

    private final int code;
}
