package com.demo.service;

import com.demo.handler.MsgHandler;
import com.demo.message.*;
import lombok.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Builder(toBuilder = true)
@Setter
@Getter
public class Gen5FMessageService {

    @Autowired
    private MsgHandler msgHandler;

    public List<Integer> gen5F10(String addr, Message5F10 msgobj) {
        List<Integer> msg = new ArrayList<>();
        msg.add(MsgHandler.DLE);
        msg.add(MsgHandler.STX);
        msg.add(MsgHandler.getSEQ());
        msg.add(Integer.parseInt(addr) >> 8);
        msg.add(Integer.parseInt(addr) % 256);
        msg.add(0);
        msg.add(14);
        msg.add(0x5F);
        msg.add(0x10);
        msg.add(msgobj.getControlStrategy());
        msg.add(msgobj.getEffecTime());
        msg.add(MsgHandler.DLE);
        msg.add(MsgHandler.ETX);
        msg.add(msgHandler.genCKS(msg));

        msg = msgHandler.sendNormalize(msg);

        return msg;
    }

    public List<Integer> gen5F14(String addr, Message5F14 msgobj) {
        int subphasecount = msgobj.getSubPhaseCount();
        int msglength = 14 + (subphasecount * 7);

        List<Integer> msg = new ArrayList<>();
        msg.add(MsgHandler.DLE);
        msg.add(MsgHandler.STX);
        msg.add(MsgHandler.getSEQ());
        msg.add(Integer.parseInt(addr) >> 8);
        msg.add(Integer.parseInt(addr) % 256);
        msg.add(msglength >> 8);
        msg.add(msglength % 256);
        msg.add(0x5F);
        msg.add(0x14);
        msg.add(msgobj.getPlanId());
        msg.add(msgobj.getSubPhaseCount());

        for (int i = 0; i < subphasecount; i++) {
            msg.add(msgobj.getDynamicArray().get(i).getMinGreen());
            msg.add(msgobj.getDynamicArray().get(i).getMaxGreen() >> 8);
            msg.add(msgobj.getDynamicArray().get(i).getMaxGreen() % 256);
            msg.add(msgobj.getDynamicArray().get(i).getYellow());
            msg.add(msgobj.getDynamicArray().get(i).getAllRed());
            msg.add(msgobj.getDynamicArray().get(i).getPedGreenFlash());
            msg.add(msgobj.getDynamicArray().get(i).getPedRed());
        }

        msg.add(MsgHandler.DLE);
        msg.add(MsgHandler.ETX);
        msg.add(msgHandler.genCKS(msg));

        msg = msgHandler.sendNormalize(msg);

        return msg;
    }

    public List<Integer> gen5F15(String addr, Message5F15 msgobj) {
        int subphasecount = msgobj.getSubPhaseCount();
        int msglength = 20 + 2 * subphasecount;

        List<Integer> msg = new ArrayList<>();
        msg.add(MsgHandler.DLE);
        msg.add(MsgHandler.STX);
        msg.add(MsgHandler.getSEQ());
        msg.add(Integer.parseInt(addr) >> 8);
        msg.add(Integer.parseInt(addr) % 256);
        msg.add(msglength >> 8);
        msg.add(msglength % 256);
        msg.add(0x5F);
        msg.add(0x15);
        msg.add(msgobj.getPlanId());
        msg.add(msgobj.getDirect());
        msg.add(Integer.parseInt(msgobj.getPhaseOrder(), 16));
        msg.add(msgobj.getSubPhaseCount());

        for (int i = 0; i < subphasecount; i++) {
            msg.add(msgobj.getDynamicArray().get(i) >> 8);
            msg.add(msgobj.getDynamicArray().get(i) % 256);
        }

        msg.add(msgobj.getCycleTime() >> 8);
        msg.add(msgobj.getCycleTime() % 256);
        msg.add(msgobj.getOffset() >> 8);
        msg.add(msgobj.getOffset() % 256);

        msg.add(MsgHandler.DLE);
        msg.add(MsgHandler.ETX);
        msg.add(msgHandler.genCKS(msg));

        msg = msgHandler.sendNormalize(msg);

        return msg;
    }

    public List<Integer> gen5F40(String addr, Message5F40 msgobj) {
        List<Integer> msg = new ArrayList<>();
        msg.add(MsgHandler.DLE);
        msg.add(MsgHandler.STX);
        msg.add(MsgHandler.getSEQ());
        msg.add(Integer.parseInt(addr) >> 8);
        msg.add(Integer.parseInt(addr) % 256);
        msg.add(0);
        msg.add(12);
        msg.add(0x5F);
        msg.add(0x40);
        msg.add(MsgHandler.DLE);
        msg.add(MsgHandler.ETX);
        msg.add(msgHandler.genCKS(msg));

        msg = msgHandler.sendNormalize(msg);

        return msg;
    }

    public List<Integer> gen5F44(String addr, Message5F44 msgobj) {
        List<Integer> msg = new ArrayList<>();
        msg.add(MsgHandler.DLE);
        msg.add(MsgHandler.STX);
        msg.add(MsgHandler.getSEQ());
        msg.add(Integer.parseInt(addr) >> 8);
        msg.add(Integer.parseInt(addr) % 256);
        msg.add(0);
        msg.add(13);
        msg.add(0x5F);
        msg.add(0x44);
        msg.add(msgobj.getPlanId());
        msg.add(MsgHandler.DLE);
        msg.add(MsgHandler.ETX);
        msg.add(msgHandler.genCKS(msg));

        msg = msgHandler.sendNormalize(msg);

        return msg;
    }

    public List<Integer> gen5F45(String addr, Message5F45 msgobj) {
        List<Integer> msg = new ArrayList<>();
        msg.add(MsgHandler.DLE);
        msg.add(MsgHandler.STX);
        msg.add(MsgHandler.getSEQ());
        msg.add(Integer.parseInt(addr) >> 8);
        msg.add(Integer.parseInt(addr) % 256);
        msg.add(0);
        msg.add(13);
        msg.add(0x5F);
        msg.add(0x45);
        msg.add(msgobj.getPlanId());
        msg.add(MsgHandler.DLE);
        msg.add(MsgHandler.ETX);
        msg.add(msgHandler.genCKS(msg));

        msg = msgHandler.sendNormalize(msg);

        return msg;
    }
}
