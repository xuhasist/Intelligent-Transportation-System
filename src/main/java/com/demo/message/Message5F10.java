package com.demo.message;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder(toBuilder = true)
public class Message5F10 extends MessageObject {
    private int controlStrategy;
    private int effecTime;
}
