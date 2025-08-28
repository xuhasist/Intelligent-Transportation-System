package com.demo.service;

import com.demo.model.its.TcInfo;
import com.demo.model.its.TcMessageLog;
import com.demo.dto.TcMessageLogDto;
import com.demo.repository.its.TcInfoRepository;
import com.demo.repository.its.TcMessageLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class TcService {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private TcMessageLogRepository tcMessageLogRepository;

    @Autowired
    private TcInfoRepository tcInfoRepository;

    @Autowired
    private ObjectMapper objectMapper;

    public Page<TcMessageLogDto> getTCMessageLog(String startDate, String endDate, int page, int size) throws JsonProcessingException {
        Page<TcMessageLog> pageResult = tcMessageLogRepository.findByLogTimeBetween(
                LocalDateTime.parse(startDate, formatter),
                LocalDateTime.parse(endDate, formatter),
                PageRequest.of(page, size)
        );

        return pageResult.map(data -> {
            TcMessageLogDto dto = TcMessageLogDto.builder()
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
    public List<TcInfo> getTCInfo() {
        //System.out.println("getTCInfo called");  // testing for assuring cache is working
        return tcInfoRepository.findAll();
    }
}
