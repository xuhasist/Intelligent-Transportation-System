package com.demo.service;

import com.demo.model.its.TCInfo;
import com.demo.model.its.TCMessageLog;
import com.demo.dto.TCMessageLogDto;
import com.demo.repository.its.TCInfoRepository;
import com.demo.repository.its.TCMessageLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class TCService {
    @Autowired
    private TCMessageLogRepository tcMessageLogRepository;

    @Autowired
    private TCInfoRepository tcInfoRepository;

    @Autowired
    private ObjectMapper objectMapper;

    public Page<TCMessageLogDto> getTCMessageLog(String startDate, String endDate, int page, int size) throws JsonProcessingException {
        Page<TCMessageLog> pageResult = tcMessageLogRepository.findByLogTimeBetween(
                objectMapper.readValue("\"" + startDate + "\"", LocalDateTime.class), // convert String to Json and then using global ObjectMapper to parse it
                objectMapper.readValue("\"" + endDate + "\"", LocalDateTime.class),
                PageRequest.of(page, size)
        );

        return pageResult.map(data -> {
            TCMessageLogDto dto = TCMessageLogDto.builder()
                    .id(data.getId())
                    .deviceId(data.getDeviceId())
                    .messageId(data.getMessageId())
                    .rawValue(data.getRawValue())
                    .noteCode(data.getNoteCode())
                    .returnResult(data.getReturnResult())
                    .logTime(data.getLogTime())
                    .build();

            try {
                if (data.getJsonValue() != null) {
                    // Parse the JSON string into a Map and set it in the DTO
                    Map<String, Object> parsedJson = objectMapper.readValue(data.getJsonValue(), new TypeReference<>() {
                    });
                    dto.setJsonValue(parsedJson);
                }
            } catch (JsonProcessingException e) {
                dto.setJsonValue(null); // or set error message
            }

            return dto;
        });
    }

    @Cacheable("tcInfo")
    public List<TCInfo> getTCInfo() {
        //System.out.println("getTCInfo called");  // testing for assuring cache is working
        return tcInfoRepository.findAll();
    }
}
