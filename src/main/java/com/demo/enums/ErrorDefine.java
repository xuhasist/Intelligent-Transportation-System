package com.demo.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorDefine {
    DataNotFound(6, "Data not found.");

    private final int statusCode;
    private final String description;
}
