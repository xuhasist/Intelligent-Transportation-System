package com.demo.controller;

import com.demo.dto.TcMessageLogDto;
import com.demo.model.its.TcInfo;
import com.demo.service.TcService;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import com.demo.exception.GlobalExceptionHandler;
import com.demo.service.JwtTokenService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@AutoConfigureMockMvc(addFilters = false)
public class TcRestControllerTest {
    private MockMvc mockMvc;

    @Mock
    private TcService tcService;

    @Mock
    private JwtTokenService jwtTokenService;

    @InjectMocks
    private TcRestController tcRestController;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(tcRestController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testGetTCMessageLog_Success() throws Exception {
        when(jwtTokenService.needsAuthentication(any(HttpServletRequest.class))).thenReturn(false);

        TcMessageLogDto log1 = new TcMessageLogDto();
        log1.setDeviceId("TC001");
        log1.setMessageId("5F03");

        TcMessageLogDto log2 = new TcMessageLogDto();
        log2.setDeviceId("TC002");
        log2.setMessageId("5F10");

        Page<TcMessageLogDto> pageData = new PageImpl<>(
                List.of(log1, log2),
                PageRequest.of(0, 100),
                1
        );

        when(tcService.getTCMessageLog(anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(pageData);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/getTCMessageLog")
                        .param("startDate", "2025-08-28 00:00:00")
                        .param("endDate", "2025-08-29 23:59:59")
                        .param("page", "0")
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].deviceId").value("TC001"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[1].messageId").value("5F10"));

        verify(tcService, times(1))
                .getTCMessageLog(anyString(), anyString(), eq(0), eq(100));
        verify(jwtTokenService, times(1)).needsAuthentication(any(HttpServletRequest.class));
    }

    @Test
    void testGetTCMessageLog_InvalidToken() throws Exception {
        when(jwtTokenService.needsAuthentication(any(HttpServletRequest.class))).thenReturn(true);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/getTCMessageLog")
                        .param("startDate", "2025-08-28 00:00:00")
                        .param("endDate", "2025-08-29 23:59:59"))
                .andExpect(status().isUnauthorized());

        verify(tcService, never()).getTCMessageLog(anyString(), anyString(), anyInt(), anyInt());
        verify(jwtTokenService, times(1)).needsAuthentication(any(HttpServletRequest.class));
    }

    @Test
    void testGetTCInfo_Success() throws Exception {
        when(jwtTokenService.needsAuthentication(any(HttpServletRequest.class))).thenReturn(false);

        TcInfo tc1 = new TcInfo();
        tc1.setTcId("TC001");

        TcInfo tc2 = new TcInfo();
        tc2.setTcId("TC002");

        List<TcInfo> tcInfoList = List.of(tc1, tc2);
        when(tcService.getTCInfo()).thenReturn(tcInfoList);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/getTCInfo"))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].tcId").value("TC001"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].tcId").value("TC002"));

        verify(tcService, times(1)).getTCInfo();
        verify(jwtTokenService, times(1)).needsAuthentication(any(HttpServletRequest.class));
    }

    @Test
    void testGetTCInfo_InvalidToken() throws Exception {
        when(jwtTokenService.needsAuthentication(any(HttpServletRequest.class))).thenReturn(true);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/getTCInfo"))
                .andExpect(status().isUnauthorized());

        verify(tcService, never()).getTCInfo();
        verify(jwtTokenService, times(1)).needsAuthentication(any(HttpServletRequest.class));
    }
}
