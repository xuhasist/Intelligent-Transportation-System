package com.demo.manager;

import com.demo.enums.MessageDefine;
import com.demo.handler.MsgHandler;

import com.demo.repository.its.TCInfoRepository;
import com.demo.service.MessageService;
import com.demo.service.SocketService;
import lombok.Getter;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class TCReceiveMessageManager {
    private static final Logger log = LoggerFactory.getLogger(TCReceiveMessageManager.class);

    @Getter
    private final Map<Socket, Map<String, List<Integer>>> responseQueues = new ConcurrentHashMap<>();

    @Getter
    private final Map<String, JSONObject> valueMap5FC0 = new ConcurrentHashMap<>();
    @Getter
    private final Map<String, JSONObject> valueMap5FC4 = new ConcurrentHashMap<>();
    @Getter
    private final Map<String, JSONObject> valueMap5FC5 = new ConcurrentHashMap<>();

    @Autowired
    private TCInfoRepository tcInfoRepository;

    @Autowired
    @Lazy
    private SocketService socketService;

    @Autowired
    private MsgHandler msgHandler;

    @Autowired
    private MessageService messageService;

    @Async
    public void run(Socket socket) {
        log.info("TCReceiveMessageManager started for socket: {}", socket.getInetAddress().getHostAddress());

        String ip = socket.getInetAddress().toString().substring(1);
        String deviceId = tcInfoRepository.findByIp(ip).getTcId();

        List<Integer> message = new ArrayList<>();      // hexadecimal
        List<String> msgstr = new ArrayList<>();        // hex string, more readable


        try (BufferedInputStream reader = new java.io.BufferedInputStream(socket.getInputStream())) {
            while (socketService.isHostConnected(ip)) {
                int character;
                while ((character = reader.read()) != -1) {
                    message.add(character);
                    msgstr.add(Integer.toHexString(character));

                    // until start with 0xaa
                    if (!message.get(0).equals(0xaa)) {
                        message.clear();
                        msgstr.clear();
                        continue;
                    }

                    int checkcode = 0;

                    if (message.size() == 8) {
                        if (message.get(1).equals(MsgHandler.ACK)) {
                            // save TcMessageLog

                            message.clear();
                            msgstr.clear();
                        }

                    } else if (message.size() == 9) {
                        if (message.get(1).equals(MsgHandler.NAK)) {
                            String key = Integer.toHexString(message.get(0)) + Integer.toHexString(message.get(1));
                            key = key + String.format("%03x", message.get(2));

                            List<Integer> copyOfMessage = new ArrayList<>(message);
                            saveToQueue(socket, key, copyOfMessage);

                            log.info("Received NAK from TC {}: {}", deviceId, msgstr);

                            message.clear();
                            msgstr.clear();
                        }
                    } else if (message.size() > 9) {
                        if ((message.get(1).equals(MsgHandler.STX)) && (
                                (!(message.get(message.size() - 4).equals(MsgHandler.DLE))
                                        && message.get(message.size() - 3).equals(MsgHandler.DLE)
                                        && message.get(message.size() - 2).equals(MsgHandler.ETX))
                                        || (message.get(message.size() - 5).equals(MsgHandler.DLE)
                                        && message.get(message.size() - 4).equals(MsgHandler.DLE)
                                        && message.get(message.size() - 3).equals(MsgHandler.DLE)
                                        && message.get(message.size() - 2).equals(MsgHandler.ETX)))) {

                            if ((checkcode = msgHandler.checkCode(message, ip)) != 0) {
                                sendNAK(message, checkcode, socket, msgstr);
                            } else {
                                sendACK(message, socket, msgstr);
                                message = msgHandler.recvNormalize(message);

                                if (message.get(7).equals(0x5f)) {
                                    int value = message.get(8);
                                    if (value == 0x03 || value == 0x08 || value == 0x0A ||
                                            value == 0x0B || value == 0x00 || value == 0x0C) {
                                        // do nothing
                                    } else {

                                        String key = generateKey(message, 7, 8);

                                        if (value == 0xc4 || value == 0xc5) {
                                            int planId = message.get(9);
                                            key = key + String.format("%02d", planId);
                                        }

                                        List<Integer> copyOfMessage = new ArrayList<>(message);
                                        saveToQueue(socket, key, copyOfMessage);

                                        if (value == 0xc5)
                                            combine5FC45FC5Messages(socket, key, copyOfMessage);

                                        if (value == 0xc0) {
                                            handle5FC0Message(deviceId, new ArrayList<>(message), new ArrayList<>(msgstr));
                                        } else if (value == 0xc4) {
                                            handle5FC4Message(deviceId, new ArrayList<>(message), new ArrayList<>(msgstr));
                                        } else if (value == 0xc5) {
                                            handle5FC5Message(deviceId, new ArrayList<>(message), new ArrayList<>(msgstr));
                                        }
                                    }
                                } else if (message.get(7).equals(0x0f)) {
                                    if (message.get(8).equals(0x80) || message.get(8).equals(0x81)) {
                                        String key = generateKey(message, 7, 8, 9, 10);
                                        List<Integer> copyOfMessage = new ArrayList<>(message);
                                        saveToQueue(socket, key, copyOfMessage);

                                        if (message.get(7).equals(0x0f) && message.get(8).equals(0x80)) {
                                            if (message.get(10).equals(0x15)) {
                                                // 使用 Optional 存儲找到的 Key
                                                Optional<String> matchingKey = responseQueues.get(socket).keySet().stream()
                                                        .filter(key_ -> key_.startsWith("0f805f14"))
                                                        .findFirst();

                                                if (matchingKey.isPresent()) {
                                                    // both correct, only reply 0f805f15
                                                    responseQueues.get(socket).remove("0f805f14");
                                                } else {
                                                    responseQueues.get(socket).remove(key);
                                                }
                                            }
                                        } else if (message.get(7).equals(0x0f) && message.get(8).equals(0x81)) {
                                            if (message.get(10).equals(0x15)) {
                                                // ignore 0f805f14
                                                responseQueues.get(socket).remove("0f805f14");
                                            }
                                        }
                                    }
                                }
                            }

                            msgstr.clear();
                            message.clear();
                        } else if (message.get(message.size() - 3).equals(MsgHandler.DLE)
                                && message.get(message.size() - 2).equals(MsgHandler.ETX)) {
                            msgstr.clear();
                            message.clear();
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.error("Error reading from socket: {}", ip, e);
            socketService.closeConnection(ip);
        }
    }

    private String generateKey(List<Integer> message, int... indices) {
        StringBuilder sb = new StringBuilder();
        for (int index : indices) {
            sb.append(String.format("%02x", message.get(index)));
        }
        return sb.toString();
    }

    private void saveToQueue(Socket socket, String key, List<Integer> copyOfMessage) {
        responseQueues
                .computeIfAbsent(socket, s -> new ConcurrentHashMap<>())
                .put(key, copyOfMessage);
    }

    public synchronized void sendNAK(List<Integer> msg, int error, Socket socket, List<String> msgStr) {
        List<Integer> nak = new ArrayList<>(Arrays.asList(
                MsgHandler.DLE, MsgHandler.NAK, msg.get(2), msg.get(3), msg.get(4),
                0x00, 0x09, error, 0
        ));

        sendControlMessage("NAK_S", msg, nak, socket, msgStr);
    }

    public synchronized void sendACK(List<Integer> msg, Socket socket, List<String> msgStr) {
        List<Integer> ack = new ArrayList<>(Arrays.asList(
                MsgHandler.DLE, MsgHandler.ACK, msg.get(2), msg.get(3), msg.get(4),
                0x00, 0x08, 0
        ));

        sendControlMessage("ACK_S", msg, ack, socket, msgStr);
    }

    private void sendControlMessage(String messageId, List<Integer> msg, List<Integer> messageList,
                                    Socket socket, List<String> msgStr) {
        try {
            String host = socket.getInetAddress().getHostAddress();
            String deviceId = tcInfoRepository.findByIp(host).getTcId();

            // 計算校驗碼
            messageList.set(messageList.size() - 1, msgHandler.genCKS(messageList));

            List<String> hexList = new ArrayList<>();
            BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());
            for (int b : messageList) {
                out.write(b);
                hexList.add(String.format("%02X", b));
            }
            out.flush();

            // save log start
            JSONObject obj = new JSONObject();
            JSONObject value = new JSONObject();

            value.put("deviceId", deviceId);
            value.put("value", msgStr.toString());

            obj.put("messageId", messageId);
            obj.put("value", value);

            messageService.saveMessageLog(obj, hexList.toString(), null, MessageDefine.chtit_to_tc.ordinal());

        } catch (Exception e) {
            log.error("Failed to send {} message", messageId, e);
        }
    }

    private void combine5FC45FC5Messages(Socket socket, String key, List<Integer> copyOfMessage) {
        Map<String, List<Integer>> socketMessageMap = responseQueues.get(socket);
        String planIdStr = key.substring(4, 6);

        Optional<String> matchingKey = socketMessageMap.keySet().stream()
                .filter(k -> k.startsWith("5fc4") && k.length() > 5 && k.substring(4, 6).equals(planIdStr))
                .findFirst();

        if (matchingKey.isPresent()) {
            List<Integer> oldMessage = socketMessageMap.get(matchingKey.get());    // get 5fc4 message
            oldMessage.addAll(copyOfMessage);   // append 5fc5 to 5fc4 data
            socketMessageMap.put(key, oldMessage);
            socketMessageMap.remove(matchingKey.get());    // remove 5fc4 message
        } else {
            socketMessageMap.remove(key);
        }
    }

    private void handle5FC0Message(String deviceId, List<Integer> message5fc0, List<String> msgstr5fc0) {
        try {
            int controlStrategy = message5fc0.get(9);
            int effectTime = message5fc0.get(10);

            JSONObject value = new JSONObject();
            value.put("ControlStrategy", controlStrategy);
            value.put("EffectTime", effectTime);
            //value.put("ControlStrategyBitMap", toBitMap(controlStrategy));
            //value.put("EffectTimeBitMap", toBitMap(effectTime));

            valueMap5FC0.put(deviceId, value);

        } catch (Exception e) {
            log.error("Error handling 5FC0 message for device {}: {}", deviceId, e.getMessage(), e);
        }
    }

    private void handle5FC4Message(String deviceId, List<Integer> message5fc4, List<String> msgstr5fc4) {
        try {
            int planId = message5fc4.get(9);
            int subPhaseCount = message5fc4.get(10);

            List<Integer> minGreen = new ArrayList<>();
            List<Integer> maxGreen = new ArrayList<>();
            List<Integer> yellow = new ArrayList<>();
            List<Integer> allRed = new ArrayList<>();
            List<Integer> pedGreenFlash = new ArrayList<>();
            List<Integer> pedRed = new ArrayList<>();

            int pos = 11;
            for (int i = 0; i < subPhaseCount; i++) {
                minGreen.add(message5fc4.get(pos++));
                maxGreen.add((message5fc4.get(pos++) << 8) | (message5fc4.get(pos++) & 0xFF));
                yellow.add(message5fc4.get(pos++));
                allRed.add(message5fc4.get(pos++));
                pedGreenFlash.add(message5fc4.get(pos++));
                pedRed.add(message5fc4.get(pos++));
            }

            JSONObject value = new JSONObject();
            value.put("planId", planId);
            value.put("subPhaseCount", subPhaseCount);
            value.put("minGreen", minGreen);
            value.put("maxGreen", maxGreen);
            value.put("yellow", yellow);
            value.put("allRed", allRed);
            value.put("pedGreenFlash", pedGreenFlash);
            value.put("pedRed", pedRed);

            valueMap5FC4.put(deviceId, value);

        } catch (Exception e) {
            log.error("Error handling 5FC4 message for device {}: {}", deviceId, e.getMessage(), e);
        }
    }

    private void handle5FC5Message(String deviceId, List<Integer> message5fc5, List<String> msgstr5fc5) {
        try {
            int planId = message5fc5.get(9);
            int direct = message5fc5.get(10);
            int phaseOrder = message5fc5.get(11);
            int subPhaseCount = message5fc5.get(12);

            List<Integer> green = new ArrayList<>();
            int pos = 13;
            for (int i = 0; i < subPhaseCount; i++) {
                green.add((message5fc5.get(pos++) << 8) | (message5fc5.get(pos++) & 0xFF));
            }

            int cycleTime = (message5fc5.get(pos++) << 8) | (message5fc5.get(pos++) & 0xFF);
            int offset = (message5fc5.get(pos++) << 8) | (message5fc5.get(pos) & 0xFF);

            JSONObject value = new JSONObject();
            value.put("planId", planId);
            value.put("direct", direct);
            value.put("phaseOrder", String.format("%02x", phaseOrder));
            value.put("subPhaseCount", subPhaseCount);
            value.put("green", green);
            value.put("cycleTime", cycleTime);
            value.put("offset", offset);

            valueMap5FC5.put(deviceId, value);

        } catch (Exception e) {
            log.error("Error handling 5FC5 message for device {}: {}", deviceId, e.getMessage(), e);
        }
    }

    private String toHex(int value) {
        return String.format("%02X", value);    // format as two-digit hexadecimal, uppercase
    }

    private String toBitMap(int value) {
        return String.format("%8s", Integer.toBinaryString(value)).replace(' ', '0');
    }
}
