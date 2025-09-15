package com.demo.message;

import lombok.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Builder(toBuilder = true)
@Setter
@Getter
public class MessageGenerator {

    @Autowired
    private MessageHandler messageHandler;

    private List<Integer> buildHeader(String addr, int length, int msgType1, int msgType2) {
        List<Integer> msg = new ArrayList<>();
        msg.add(MessageHandler.DLE);
        msg.add(MessageHandler.STX);
        msg.add(MessageHandler.getSEQ());
        msg.add(Integer.parseInt(addr) >> 8);
        msg.add(Integer.parseInt(addr) % 256);
        msg.add(length >> 8);
        msg.add(length % 256);
        msg.add(msgType1);
        msg.add(msgType2);
        return msg;
    }

    private List<Integer> buildFooter(List<Integer> msg) {
        msg.add(MessageHandler.DLE);
        msg.add(MessageHandler.ETX);
        msg.add(messageHandler.genCKS(msg));
        return messageHandler.sendNormalize(msg);
    }

    public List<Integer> gen5F10(String addr, Message5F10 msgobj) {
        List<Integer> msg = buildHeader(addr, 14, 0x5F, 0x10);
        msg.add(msgobj.getControlStrategy());
        msg.add(msgobj.getEffecTime());
        return buildFooter(msg);
    }

    public List<Integer> gen5F14(String addr, Message5F14 msgobj) {
        int subphasecount = msgobj.getSubPhaseCount();
        int msglength = 14 + (subphasecount * 7);

        List<Integer> msg = buildHeader(addr, msglength, 0x5F, 0x14);
        msg.add(msgobj.getPlanId());
        msg.add(subphasecount);

        for (int i = 0; i < subphasecount; i++) {
            msg.add(msgobj.getDynamicArray().get(i).getMinGreen());
            msg.add(msgobj.getDynamicArray().get(i).getMaxGreen() >> 8);
            msg.add(msgobj.getDynamicArray().get(i).getMaxGreen() % 256);
            msg.add(msgobj.getDynamicArray().get(i).getYellow());
            msg.add(msgobj.getDynamicArray().get(i).getAllRed());
            msg.add(msgobj.getDynamicArray().get(i).getPedGreenFlash());
            msg.add(msgobj.getDynamicArray().get(i).getPedRed());
        }

        return buildFooter(msg);
    }

    public List<Integer> gen5F15(String addr, Message5F15 msgobj) {
        int subphasecount = msgobj.getSubPhaseCount();
        int msglength = 20 + 2 * subphasecount;

        List<Integer> msg = buildHeader(addr, msglength, 0x5F, 0x15);
        msg.add(msgobj.getPlanId());
        msg.add(msgobj.getDirect());
        msg.add(Integer.parseInt(msgobj.getPhaseOrder(), 16));
        msg.add(subphasecount);

        for (int i = 0; i < subphasecount; i++) {
            msg.add(msgobj.getDynamicArray().get(i) >> 8);
            msg.add(msgobj.getDynamicArray().get(i) % 256);
        }

        msg.add(msgobj.getCycleTime() >> 8);
        msg.add(msgobj.getCycleTime() % 256);
        msg.add(msgobj.getOffset() >> 8);
        msg.add(msgobj.getOffset() % 256);

        return buildFooter(msg);
    }

    public List<Integer> gen5F18(String addr, Message5F18 msgobj) {
        List<Integer> msg = buildHeader(addr, 13, 0x5F, 0x18);
        msg.add(msgobj.getPlanId());
        return buildFooter(msg);
    }

    public List<Integer> gen5F40(String addr, Message5F40 msgobj) {
        List<Integer> msg = buildHeader(addr, 12, 0x5F, 0x40);
        return buildFooter(msg);
    }

    public List<Integer> gen5F44(String addr, Message5F44 msgobj) {
        List<Integer> msg = buildHeader(addr, 13, 0x5F, 0x44);
        msg.add(msgobj.getPlanId());
        return buildFooter(msg);
    }

    public List<Integer> gen5F45(String addr, Message5F45 msgobj) {
        List<Integer> msg = buildHeader(addr, 13, 0x5F, 0x45);
        msg.add(msgobj.getPlanId());
        return buildFooter(msg);
    }
}
