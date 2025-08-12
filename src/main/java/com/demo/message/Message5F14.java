package com.demo.message;

import lombok.*;

import java.util.*;

@Getter
@Setter
@Builder(toBuilder = true)
public class Message5F14 extends MessageObject {
    private int planId;
    private int subPhaseCount;

    @Builder.Default
    private List<MessageSubPhases> dynamicArray = new ArrayList<>();
}
