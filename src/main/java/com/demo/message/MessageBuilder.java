package com.demo.message;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class MessageBuilder {

    public Message5F10 buildMessage5F10(JSONObject obj) {
        JSONObject value = obj.getJSONObject("value");

        int controlStrategy = value.getInt("controlStrategy");
        int effectTime = value.getInt("effectTime");

        return Message5F10.builder()
                .controlStrategy(controlStrategy)
                .effecTime(effectTime)
                .build();
    }

    public Message5F14 buildMessage5F14(JSONObject obj) {
        JSONObject value = obj.getJSONObject("value");

        int planId = value.getInt("planId");
        int subPhaseCount = value.getInt("subPhaseCount");
        JSONArray subPhasesJsonArray = value.getJSONArray("subPhases");

        List<MessageSubPhases> dynamicArray_14 = new ArrayList<>();
        for (int i = 0; i < subPhaseCount; i++) {
            MessageSubPhases subPhasesMessage = MessageSubPhases.builder()
                    .minGreen(subPhasesJsonArray.getJSONObject(i).getInt("minGreen"))
                    .maxGreen(subPhasesJsonArray.getJSONObject(i).getInt("maxGreen"))
                    .yellow(subPhasesJsonArray.getJSONObject(i).getInt("yellow"))
                    .allRed(subPhasesJsonArray.getJSONObject(i).getInt("allRed"))
                    .pedGreenFlash(subPhasesJsonArray.getJSONObject(i).getInt("pedGreenFlash"))
                    .pedRed(subPhasesJsonArray.getJSONObject(i).getInt("pedRed"))
                    .build();

            dynamicArray_14.add(subPhasesMessage);
        }

        return Message5F14.builder()
                .planId(planId)
                .subPhaseCount(subPhaseCount)
                .dynamicArray(dynamicArray_14)
                .build();
    }

    public Message5F15 buildMessage5F15(JSONObject obj) {
        JSONObject value = obj.getJSONObject("value");

        int planId = value.getInt("planId");
        int direct = value.getInt("direct");
        String phaseOrder = value.getString("phaseOrder");
        int subPhaseCount = value.getInt("subPhaseCount");
        int cycleTime = value.getInt("cycleTime");
        int offset = value.getInt("offset");
        JSONArray subPhasesJsonArray = value.getJSONArray("subPhases");

        List<Integer> dynamicArray_15 = new ArrayList<>();
        for (int i = 0; i < subPhaseCount; i++) {
            dynamicArray_15.add(subPhasesJsonArray.getJSONObject(i).getInt("green"));
        }

        return Message5F15.builder()
                .planId(planId)
                .direct(direct)
                .phaseOrder(phaseOrder)
                .subPhaseCount(subPhaseCount)
                .dynamicArray(dynamicArray_15)
                .cycleTime(cycleTime)
                .offset(offset)
                .build();
    }

    public Message5F18 buildMessage5F18(JSONObject obj) {
        JSONObject value = obj.getJSONObject("value");
        int planId = value.getInt("planId");

        return Message5F18.builder()
                .planId(planId)
                .build();
    }

    public Message5F40 buildMessage5F40(JSONObject obj) {
        return Message5F40.builder().build();
    }

    public Message5F44 buildMessage5F44(JSONObject obj) {
        JSONObject value = obj.getJSONObject("value");

        int planId = value.getInt("planId");

        return Message5F44.builder()
                .planId(planId)
                .build();
    }

    public Message5F45 buildMessage5F45(JSONObject obj) {
        JSONObject value = obj.getJSONObject("value");

        int planId = value.getInt("planId");

        return Message5F45.builder()
                .planId(planId)
                .build();
    }
}
