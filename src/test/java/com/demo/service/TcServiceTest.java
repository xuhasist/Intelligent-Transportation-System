package com.demo.service;

import com.demo.dto.TcMessageLogDto;
import com.demo.model.its.TcInfo;
import com.demo.model.its.TcMessageLog;
import com.demo.repository.its.TcInfoRepository;
import com.demo.repository.its.TcMessageLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import com.fasterxml.jackson.core.type.TypeReference;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TcServiceTest {
    @InjectMocks
    private TcService tcService;

    @Mock
    private TcMessageLogRepository tcMessageLogRepository;

    @Mock
    private TcInfoRepository tcInfoRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Test
    void testGetTCMessageLog_withValidJson() throws Exception {
        String startDate = "2025-10-28 00:00:00";
        String endDate = "2025-10-28 23:59:59";
        int page = 0;
        int size = 10;

        TcMessageLog log = new TcMessageLog();
        log.setId(1L);
        log.setDeviceId("dev1");
        log.setMessageId("msg1");
        log.setRawValue("raw");
        log.setReturnResult("OK");
        log.setLogTime(LocalDateTime.of(2025, 10, 28, 12, 0));
        log.setJsonValue("{\"key\":\"value\"}");

        Page<TcMessageLog> pageResult = new PageImpl<>(List.of(log));

        when(tcMessageLogRepository.findByLogTimeBetween(
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                any(PageRequest.class)
        )).thenReturn(pageResult);

        // simulate objectMapper parsing JSON
        Map<String, Object> parsedMap = new HashMap<>();
        parsedMap.put("key", "value");
        when(objectMapper.readValue(eq("{\"key\":\"value\"}"), any(TypeReference.class)))
                .thenReturn(parsedMap);

        Page<TcMessageLogDto> result = tcService.getTCMessageLog(startDate, endDate, page, size);

        assertEquals(1, result.getTotalElements());
        TcMessageLogDto dto = result.getContent().getFirst();
        assertEquals("dev1", dto.getDeviceId());
        assertEquals("msg1", dto.getMessageId());
        assertEquals(parsedMap, dto.getJsonValue());
    }

    @Test
    void testGetTCMessageLog_withInvalidJson() throws Exception {
        String startDate = "2025-10-28 00:00:00";
        String endDate = "2025-10-28 23:59:59";

        TcMessageLog log = new TcMessageLog();
        log.setJsonValue("invalid json");

        Page<TcMessageLog> pageResult = new PageImpl<>(List.of(log));
        when(tcMessageLogRepository.findByLogTimeBetween(any(), any(), any())).thenReturn(pageResult);

        when(objectMapper.readValue(anyString(), any(TypeReference.class)))
                .thenThrow(new JsonProcessingException("error") {
                });

        Page<TcMessageLogDto> result = tcService.getTCMessageLog(startDate, endDate, 0, 10);

        TcMessageLogDto dto = result.getContent().get(0);
        assertNull(dto.getJsonValue());  // JSON parse fail, return null
    }

    @Test
    void testGetTCInfo() {
        TcInfo info1 = new TcInfo();
        TcInfo info2 = new TcInfo();
        when(tcInfoRepository.findAll()).thenReturn(List.of(info1, info2));

        List<TcInfo> result = tcService.getTCInfo();

        assertEquals(2, result.size());
        verify(tcInfoRepository, times(1)).findAll();
    }
}
