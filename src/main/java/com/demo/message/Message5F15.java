package com.demo.message;

import lombok.*;

import java.util.*;

@Getter
@Setter
@Builder(toBuilder = true)
public class Message5F15 extends MessageObject {
    private int planId;
    private int direct;

    @Builder.Default
    private String phaseOrder = "";

    private int subPhaseCount;

    @Builder.Default
    private List<Integer> dynamicArray = new ArrayList<>();

    private int cycleTime;
    private int offset;
}
