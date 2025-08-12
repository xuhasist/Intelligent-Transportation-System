package com.demo.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.*;

@Getter
@AllArgsConstructor
public enum NakDefine {
    nak1("Checksum error", 1),
    nak2("Frame error", 2),
    nak3("Address error", 4),
    nak4("Length error", 8);

    private final String description;
    private final int value;

    private static final Map<Integer, String> valueToLabelMap = new HashMap<>();

    static {
        for (NakDefine nak : NakDefine.values()) {
            valueToLabelMap.put(nak.getValue(), nak.getDescription());
        }
    }

    public static String getDescriptionByValue(int value) {
        return valueToLabelMap.getOrDefault(value, "");
    }
}
