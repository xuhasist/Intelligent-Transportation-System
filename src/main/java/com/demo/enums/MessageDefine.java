package com.demo.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MessageDefine {
    // define the data flow direction
    chtit_to_tc("CHTIT → TC"),
    chtit_to_mqtt("CHTIT → MQTT"),
    mqtt_to_chtit("MQTT → CHTIT"),
    tc_to_chtit("TC → CHTIT");

    private final String description;
}
