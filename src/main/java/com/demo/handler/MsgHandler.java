package com.demo.handler;

import com.demo.repository.its.TCInfoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.*;

@Component
public class MsgHandler {
    private static final Logger log = LoggerFactory.getLogger(MsgHandler.class);

    public final static int DLE = 0xaa;
    public final static int STX = 0xbb;
    public final static int ETX = 0xcc;
    public final static int ACK = 0xdd;
    public final static int NAK = 0xee;

    @Autowired
    private TCInfoRepository tcInfoRepository;

    private static final int MAX_SEQ = 255;
    private static final AtomicInteger SEQ = new AtomicInteger(0);

    static public int getSEQ() {
        while (true) {
            int current = SEQ.get();
            int next = (current >= MAX_SEQ) ? 1 : current + 1;
            if (SEQ.compareAndSet(current, next)) {
                return next;
            }
            // if failed, retry
        }
    }

    public int genCKS(List<Integer> msg) {
        int cks = 0;
        for (Integer integer : msg) {
            cks = cks ^ integer;
        }

        return cks;
    }

    public int checkCKS(List<Integer> msg) {
        int cks = 0;
        for (int i = 0; i < msg.size() - 1; i++) {
            cks = cks ^ msg.get(i);
        }

        return cks;
    }

    public int checkCode(List<Integer> msg, String ip) {
        String host = tcInfoRepository.findByIp(ip).getTcId();
        String addr = String.valueOf(tcInfoRepository.findByIp(ip).getAddr());

        int checkcode = 0;

        try {
            if (!(msg.getLast().equals(checkCKS(msg)))) {
                log.info("Device: {}, CKS error, received: {}, expected: {}", host, msg.getLast(), genCKS(msg));
                checkcode = 1;
            } else if (!msg.get(0).equals(DLE) || !msg.get(msg.size() - 3).equals(DLE) || !msg.get(msg.size() - 2).equals(ETX)) {
                log.info("Device: {}, Frame error, msg = {}", host, msg);
                checkcode = 2;
            } else if ((msg.get(5) * 16 * 16 + msg.get(6)) != msg.size()) {
                log.info("Device: {}, LEN error, LEN = {}, msg size = {}", host, msg.get(5) * 16 * 16 + msg.get(6), msg.size());
                checkcode = 4;
            } else if ((msg.get(3) * 16 * 16 + msg.get(4)) != Integer.parseInt(addr)) {
                log.info("Device: {}, ADDR error, ADDR = {}, Database addr = {}", host, msg.get(3) * 16 * 16 + msg.get(4), Integer.parseInt(addr));
                checkcode = 8;
            }
        } catch (Exception e) {
            log.error("Device: {}, error: {}", host, e.getMessage());
        }
        return checkcode;
    }

    public List<Integer> sendNormalize(List<Integer> msg) {
        List<Integer> message = new ArrayList<>();
        int msgLength = msg.get(5) * 256 + msg.get(6);
        int addMsg = 0;

        for (int i = 0; i < msg.size(); i++) {
            if ((i >= 7) && i < (msgLength - 3) && msg.get(i) == DLE) {
                message.add(msg.get(i));
                addMsg++;
            }
            message.add(msg.get(i));
        }
        msgLength = msgLength + addMsg;
        message.set(5, (msgLength >> 8));
        message.set(6, (msgLength % 256));
        message.removeLast();
        message.add(genCKS(message));

        return message;

    }

    public List<Integer> recvNormalize(List<Integer> msg) {
        List<Integer> message = new ArrayList<>();
        message.add(msg.getFirst());
        for (int i = 1; i < msg.size(); i++) {
            if (msg.get(i) == DLE) {
                if (msg.get(i - 1) != DLE) {
                    message.add(msg.get(i));
                }
            } else {
                message.add(msg.get(i));
            }
        }
        return message;

    }
}
