package com.demo.service;

import com.demo.message.*;
import com.demo.model.its.TcMessageLog;
import com.demo.repository.its.TcMessageLogRepository;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MessageService {
    private static final Logger log = LoggerFactory.getLogger(MessageService.class);

    @Autowired
    private TcMessageLogRepository tcMessageLogRepository;

    @Autowired
    private MessageBuilder messageBuilder;

    @Autowired
    private MessageGenerator messageGenerator;

    public MessageObject buildMessage(JSONObject obj, String command) {
        return switch (command) {
            case "5f10" -> messageBuilder.buildMessage5F10(obj);
            case "5f14" -> messageBuilder.buildMessage5F14(obj);
            case "5f15" -> messageBuilder.buildMessage5F15(obj);
            case "5f18" -> messageBuilder.buildMessage5F18(obj);
            case "5f40" -> messageBuilder.buildMessage5F40(obj);
            case "5f44" -> messageBuilder.buildMessage5F44(obj);
            case "5f45" -> messageBuilder.buildMessage5F45(obj);
            default -> null;
        };
    }

    public List<Integer> generateMessage(String addr, String command, MessageObject msgobj) {
        return switch (command) {
            case "5f10" -> messageGenerator.gen5F10(addr, (Message5F10) msgobj);
            case "5f14" -> messageGenerator.gen5F14(addr, (Message5F14) msgobj);
            case "5f15" -> messageGenerator.gen5F15(addr, (Message5F15) msgobj);
            case "5f18" -> messageGenerator.gen5F18(addr, (Message5F18) msgobj);
            case "5f40" -> messageGenerator.gen5F40(addr, (Message5F40) msgobj);
            case "5f44" -> messageGenerator.gen5F44(addr, (Message5F44) msgobj);
            case "5f45" -> messageGenerator.gen5F45(addr, (Message5F45) msgobj);
            default -> null;
        };
    }

    public TcMessageLog saveMessageLog(JSONObject obj, String rawValue, String returnResult, int noteCode) {
        String deviceId = obj.getJSONObject("value").getString("deviceId");
        String messageId = obj.getString("messageId").toUpperCase();
        String jsonValue = obj.getJSONObject("value").toString();

        TcMessageLog tcLog = null;
        try {
            tcLog = TcMessageLog.builder()
                    .deviceId(deviceId)
                    .messageId(messageId.toUpperCase())
                    .jsonValue(jsonValue)
                    .noteCode(noteCode)
                    .build();
            tcMessageLogRepository.save(tcLog);

            if (rawValue != null) tcLog.setRawValue(rawValue);
            if (returnResult != null) tcLog.setReturnResult(returnResult);

            tcMessageLogRepository.save(tcLog);
        } catch (Exception e) {
            log.error("save TcMessageLog fail, ", e);
        }

        return tcLog;
    }
}
