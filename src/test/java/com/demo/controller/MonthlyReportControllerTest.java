package com.demo.controller;

import com.demo.dto.OverallPerformanceDto;
import com.demo.dto.PeriodPerformanceDto;
import com.demo.exception.GlobalExceptionHandler;
import com.demo.service.JwtTokenService;
import com.demo.service.MonthlyReportService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(addFilters = false)
public class MonthlyReportControllerTest {

    private MockMvc mockMvc;

    @InjectMocks
    private MonthlyReportController monthlyReportController;

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private MonthlyReportService monthlyReportService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(monthlyReportController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testGetTotalPerformance_Success() throws Exception {
        when(jwtTokenService.needsAuthentication(any(HttpServletRequest.class))).thenReturn(false);

        List<List<Map<String, Object>>> mockData = List.of(
                List.of(Map.of("平假日", "平日", "時段", "08:00")),
                List.of(Map.of("平假日", "假日", "時段", "10:00"))
        );

        when(monthlyReportService.callSpRoadTrafficSum(anyString(), anyString()))
                .thenReturn(mockData);

        when(monthlyReportService.extractTableData(anyList(), eq(OverallPerformanceDto.class), eq(0)))
                .thenReturn(List.of(new OverallPerformanceDto()));

        when(monthlyReportService.extractTableData(anyList(), eq(PeriodPerformanceDto.class), eq(1)))
                .thenReturn(List.of(new PeriodPerformanceDto()));

        doNothing().when(monthlyReportService)
                .exportExcel(anyList(), anyList(), anyString(), any(HttpServletResponse.class));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/monthlyReport/getTotalPerformance")
                        .param("yearMonth", "2025-06"))
                .andExpect(status().isOk());

        verify(jwtTokenService, times(1)).needsAuthentication(any(HttpServletRequest.class));
        verify(monthlyReportService, times(1)).callSpRoadTrafficSum(anyString(), anyString());
        verify(monthlyReportService, times(1))
                .extractTableData(anyList(), eq(OverallPerformanceDto.class), eq(0));
        verify(monthlyReportService, times(1))
                .extractTableData(anyList(), eq(PeriodPerformanceDto.class), eq(1));
        verify(monthlyReportService, times(1))
                .exportExcel(anyList(), anyList(), eq("202506"), any(HttpServletResponse.class));
    }

    @Test
    void testGetTotalPerformance_InvalidToken() throws Exception {
        when(jwtTokenService.needsAuthentication(any(HttpServletRequest.class))).thenReturn(true);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/monthlyReport/getTotalPerformance")
                        .param("yearMonth", "2025-06"))
                .andExpect(status().isUnauthorized());

        verify(jwtTokenService, times(1)).needsAuthentication(any(HttpServletRequest.class));
        verifyNoInteractions(monthlyReportService);
    }
}
