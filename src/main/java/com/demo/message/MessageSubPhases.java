package com.demo.message;

import lombok.*;

@Getter
@Setter
@Builder(toBuilder = true)
public class MessageSubPhases {
    private int minGreen;
    private int maxGreen;
    private int yellow;
    private int allRed;
    private int pedGreenFlash;
    private int pedRed;
}
