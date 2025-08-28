package com.demo.manager;

import com.demo.enums.MessageDefine;
import com.demo.enums.NakDefine;
import com.demo.repository.its.TcInfoRepository;
import com.demo.message.*;
import com.demo.service.MessageService;
import com.demo.message.MessageGenerator;
import com.demo.service.MqttClientService;
import com.demo.service.SocketService;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

@Service
public class TcSendMessageManager {
    private static final Logger log = LoggerFactory.getLogger(TcSendMessageManager.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");

    private final String topic_tc_publish_prefix = "/v3/demo/device/TC/";

    @Autowired
    private TcInfoRepository tcInfoRepository;

    @Autowired
    private MessageGenerator messageGenerator;

    @Autowired
    @Lazy
    private MqttClientService mqttClientService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private SocketService socketService;

    @Autowired
    private TcReceiveMessageManager tcReceiveMessageManager;

    @Async
    public void run(String message) {
        log.info("TCReceiveMessageManager started for message: {}", message);

        JSONObject obj = new JSONObject(message);
        String messageId = obj.getString("messageId").toUpperCase();
        messageService.saveMessageLog(obj, null, null, MessageDefine.mqtt_to_chtit.ordinal());

        if (messageId.equals("5F10")) {
            handle5F10Message(obj);
        } else if (messageId.equals("5F15")) {
            handle5F15Message(obj);
        } else if (messageId.equals("5F18")) {
            handle5F18Message(obj);
        } else if (messageId.equals("5F40")) {
            handle5F40Message(obj);
        } else if (messageId.equals("5F45")) {
            handle5F45Message(obj);
        }

        // Other commands are handled but not shown
    }

    public boolean handle5F10Message(JSONObject obj) {
        String deviceId = obj.getJSONObject("value").getString("deviceId");

        try {
            Message5F10 msg5F10 = (Message5F10) messageService.buildMessage(obj, "5f10");

            String successKey = "0f805f10";
            String failKey = "0f815f10";

            return sendMessage(deviceId, "5f10", msg5F10, successKey, failKey);

        } catch (Exception e) {
            log.error("handle5F10Message failed, deviceIds = {} ", deviceId, e);
        }

        return false;
    }

    public boolean handle5F15Message(JSONObject obj) {
        String deviceId = obj.getJSONObject("value").getString("deviceId");

        try {
            Message5F14 msg5F14 = (Message5F14) messageService.buildMessage(obj, "5f14");
            Message5F15 msg5F15 = (Message5F15) messageService.buildMessage(obj, "5f15");

            String successKey_14 = "0f805f14";
            String failKey_14 = "0f815f14";

            String successKey_15 = "0f805f15";
            String failKey_15 = "0f815f15";

            if (sendMessage(deviceId, "5f14", msg5F14, successKey_14, failKey_14)) {
                return sendMessage(deviceId, "5f15", msg5F15, successKey_15, failKey_15);
            }

        } catch (Exception e) {
            log.error("handle5F15Message failed, deviceIds = {} ", deviceId, e);
        }

        return false;
    }

    public boolean handle5F18Message(JSONObject obj) {
        String deviceId = obj.getJSONObject("value").getString("deviceId");

        try {
            Message5F18 msg5F18 = (Message5F18) messageService.buildMessage(obj, "5f18");

            String successKey = "0f805f18";
            String failKey = "0f815f18";

            return sendMessage(deviceId, "5f18", msg5F18, successKey, failKey);

        } catch (Exception e) {
            log.error("handle5F18Message failed, deviceIds = {} ", deviceId, e);
        }

        return false;
    }

    public boolean handle5F40Message(JSONObject obj) {
        String deviceId = obj.getJSONObject("value").getString("deviceId");

        try {
            Message5F40 msg5F40 = (Message5F40) messageService.buildMessage(obj, "5f40");

            String successKey = "5fc0";
            String failKey = "";

            return sendMessage(deviceId, "5f40", msg5F40, successKey, failKey);

        } catch (Exception e) {
            log.error("handle5F40Message failed, deviceIds = {} ", deviceId, e);
        }

        return false;
    }

    public boolean handle5F45Message(JSONObject obj) {
        String deviceId = obj.getJSONObject("value").getString("deviceId");
        int planId = obj.getJSONObject("value").getInt("planId");

        try {
            Message5F44 msg5F44 = (Message5F44) messageService.buildMessage(obj, "5f44");
            Message5F45 msg5F45 = (Message5F45) messageService.buildMessage(obj, "5f45");

            String successKey_c4 = "5fc4" + String.format("%02d", planId);
            String successKey_c5 = "5fc5" + String.format("%02d", planId);
            String failKey = "";

            if (sendMessage(deviceId, "5f44", msg5F44, successKey_c4, failKey)) {
                return sendMessage(deviceId, "5f45", msg5F45, successKey_c5, failKey);
            }

        } catch (Exception e) {
            log.error("handle5F45Message failed, deviceIds = {} ", deviceId, e);
        }

        return false;
    }

    void publish5FC0Message(String deviceId, List<Integer> message) {
        try {
            ZonedDateTime currentTime = ZonedDateTime.now();
            String messageTime = currentTime.format(formatter);
            String topic = topic_tc_publish_prefix + deviceId;

            int ControlStrategy = message.get(9);
            int EffectTime = message.get(10);

            JSONObject returnData = new JSONObject();
            returnData.put("messageTime", messageTime);

            JSONObject value = new JSONObject();
            value.put("deviceId", deviceId);
            value.put("controlStrategy", ControlStrategy);
            value.put("effectTime", EffectTime);
            value.put("status", 1);

            StringBuilder resData = new StringBuilder();
            for (int i = 7; i < 11; i++) {
                resData.append(String.format("%2s", Integer.toHexString(message.get(i))).replace(' ', '0'));
            }

            value.put("resData", resData);
            returnData.put("messageId", "5FC0");

            returnData.put("value", value);

            boolean success = mqttClientService.publish(1, false, topic, returnData.toString());
            messageService.saveMessageLog(returnData, null, success ? null : "Publish success but return false", MessageDefine.chtit_to_mqtt.ordinal());

        } catch (Exception e) {
            log.error("Failed to publish 5FC0 message", e);
        }
    }

    void publish0F80or0F81Message(String deviceId, List<Integer> message) {
        try {
            ZonedDateTime currentTime = ZonedDateTime.now();
            String messageTime = currentTime.format(formatter);
            String topic = topic_tc_publish_prefix + deviceId;

            int msg7 = message.get(7);
            int msg8 = message.get(8);

            String commandId = String.format("%02X%02X", message.get(9), message.get(10));
            if (commandId.equalsIgnoreCase("5F14")) {
                commandId = "5F15"; //	change 5F14 to 5F15
            }

            JSONObject returnData = new JSONObject();
            returnData.put("messageTime", messageTime);

            JSONObject value = new JSONObject();
            value.put("deviceId", deviceId);
            value.put("commandId", commandId);
            value.put("status", 1);

            if (msg7 == 0x0F && msg8 == 0x80) {
                StringBuilder resData = new StringBuilder();
                for (int i = 7; i < 11; i++) {
                    resData.append(String.format("%2s", Integer.toHexString(message.get(i))).replace(' ', '0'));
                }

                value.put("resData", resData);
                returnData.put("messageId", "0F80");
            } else if (msg7 == 0x0F && msg8 == 0x81) {
                StringBuilder resData = new StringBuilder();
                for (int i = 7; i < 13; i++) {
                    resData.append(String.format("%2s", Integer.toHexString(message.get(i))).replace(' ', '0'));
                }

                String errorCode = resData.length() >= 10 ? resData.substring(8, 10) : "";
                String paramNumber = resData.length() >= 12 ? resData.substring(10, 12) : "";

                value.put("errorCode", errorCode);
                value.put("parameterNumber", paramNumber);
                value.put("resData", commandId.equals("5F15") ? "0F815F15" : resData);

                returnData.put("messageId", "0F81");
            }

            returnData.put("value", value);

            boolean success = mqttClientService.publish(1, false, topic, returnData.toString());
            messageService.saveMessageLog(returnData, null, success ? null : "Publish success but return false", MessageDefine.chtit_to_mqtt.ordinal());

        } catch (Exception e) {
            log.error("Failed to publish 0F80 or 0F81 message", e);
        }
    }

    public boolean sendMessage(String deviceId, String command, MessageObject msgobj, String successKey, String failKey) {
        String ip = tcInfoRepository.findByTcId(deviceId).getIp();
        try {
            if (!socketService.isHostConnected(ip)) {
                log.warn("TC not connected: {}, {}", ip, command);
                return false;
            }

            List<Integer> message = genMsg(ip, command, msgobj);
            if (message == null) {
                log.warn("genMsg returns null for command: {}", command);
                return false;
            }

            Socket socket = socketService.getConnection(ip);
            return retrySendWithResponse(socket, message, deviceId, command, successKey, failKey);

        } catch (IOException | InterruptedException e) {
            log.error("Failed to send message: {}, {}", ip, command, e);
            return false;
        }
    }

    private boolean retrySendWithResponse(Socket socket, List<Integer> msg, String deviceId, String command, String successKey, String failKey) throws IOException, InterruptedException {
        BufferedOutputStream out = new java.io.BufferedOutputStream(socket.getOutputStream());
        String seq = String.format("%03x", msg.get(2));
        String nakKey = "aaee".substring(0, 4) + seq;   // nak key is aaee

        int maxRetries = 3;
        for (int retry = 0; retry < maxRetries; retry++) {
            sendMessageToSocket(out, msg, deviceId, command);

            try {
                // wait for 5 secs
                List<Integer> response = waitForSpecificResponse(socket, 5000, successKey, failKey, nakKey).get();
                if (response != null) {
                    return handleResponse(response, command, deviceId, socket, successKey);
                }

                log.warn("No response, retry {} for {}:{}", retry + 1, deviceId, command);
            } catch (InterruptedException | ExecutionException e) {
                log.error("Exception occurred during CompletableFuture execution for device {} and command {}.", deviceId, command, e.getCause());
            }

            Thread.sleep(100);
        }

        return false;
    }

    private void sendMessageToSocket(BufferedOutputStream out, List<Integer> msg, String deviceId, String command) throws IOException {
        List<String> msgstr = new ArrayList<>();  // hex string, more readable

        for (Integer b : msg) {
            out.write(b);
            msgstr.add(Integer.toHexString(b));
        }
        out.flush();

        // save log start
        JSONObject obj = new JSONObject();
        JSONObject value = new JSONObject();

        value.put("deviceId", deviceId);
        value.put("value", "");

        obj.put("value", value);
        obj.put("messageId", command);

        messageService.saveMessageLog(obj, msgstr.toString(), null, MessageDefine.chtit_to_tc.ordinal());
        // save log end
    }

    private boolean handleResponse(List<Integer> response, String command, String deviceId, Socket socket, String successKey) {
        if (response.get(0) == 0xAA && response.get(1) == 0xEE) {     // NAK
            String msg = "TC respond NAK: " + NakDefine.getDescriptionByValue(response.get(7));
            return false;
        }

        switch (command) {
            case "5f14" -> {
                // only handle 0F81 setting failure
                // if 0F80 success, do nothing and wait for 5F15
                if (response.get(7) == 0x0F && response.get(8) == 0x81) {
                    publish0F80or0F81Message(deviceId, response);
                    tcReceiveMessageManager.getResponseQueues().get(socket).remove(successKey);
                    return false;
                }
            }
            case "5f10", "5f15", "5f18" -> publish0F80or0F81Message(deviceId, response);
            case "5f40" -> publish5FC0Message(deviceId, response);
        }

        // 5f14 has to wait for 5f15, 5fc4 has to wait for 5fc5
        if (!command.equals("5f14") && !command.equals("5f44")) {
            tcReceiveMessageManager.getResponseQueues().get(socket).remove(successKey);
        }

        return true;
    }

    private CompletableFuture<List<Integer>> waitForSpecificResponse(Socket socket, int timeoutMillis, String successKey, String failKey, String nakKey) throws InterruptedException {
        CompletableFuture<List<Integer>> futureResult = new CompletableFuture<>();
        String[] keys = {successKey, failKey, nakKey};

        long startTime = System.currentTimeMillis();

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        ScheduledFuture<?> scheduledTask = scheduler.scheduleAtFixedRate(() -> {
            try {
                Map<String, List<Integer>> map = tcReceiveMessageManager.getResponseQueues().get(socket);

                if (map != null) {
                    for (String key : keys) {
                        if (map.containsKey(key)) {
                            List<Integer> result = map.get(key);
                            futureResult.complete(result);       // CompletableFuture completes
                            return;
                        }
                    }
                }

                // Timeout check
                if (System.currentTimeMillis() - startTime > timeoutMillis) {
                    futureResult.complete(null);             // timeout null
                }
            } catch (Exception e) {
                futureResult.completeExceptionally(e);
            }
        }, 0, 100, TimeUnit.MILLISECONDS);

        // When completableFuture completes，cancel scheduled task
        futureResult.whenComplete((res, ex) -> scheduledTask.cancel(false));

        return futureResult;
    }

    private List<Integer> genMsg(String host, String command, MessageObject msgobj) {
        String addr = String.valueOf(tcInfoRepository.findByIp(host).getAddr());
        return messageService.generateMessage(addr, command, msgobj);
    }
}
