package com.demo.controller;

import com.demo.enums.AuthDefine;
import com.demo.service.DeviceStatusService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import com.demo.exception.CustomException;
import com.demo.exception.GlobalExceptionHandler;
import com.demo.service.JwtTokenService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;


@AutoConfigureMockMvc(addFilters = false)
public class DeviceStatusControllerTest {
    @Mock
    private DeviceStatusService deviceStatusService;

    @Mock
    private JwtTokenService jwtTokenService;

    @InjectMocks
    private DeviceStatusController deviceStatusController;

    private MockMvc mockMvc;

    private final String startDate = "2025-08-28";
    private final String endDate = "2025-08-29";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders
                .standaloneSetup(deviceStatusController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testGetDeviceStatus_Success() throws Exception {
        Map<String, Object> record = Map.of("deviceId", "123", "status", "online");
        Page<Map<String, Object>> pageResult = new PageImpl<>(
                List.of(record),
                PageRequest.of(0, 100),
                1
        );

        Mockito.when(jwtTokenService.needsAuthentication(Mockito.any(HttpServletRequest.class)))
                .thenReturn(false);
        Mockito.when(deviceStatusService.getDeviceStatus(startDate, endDate, 0, 100))
                .thenReturn(pageResult);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/device-status")
                        .param("startDate", startDate)
                        .param("endDate", endDate)
                        .param("page", "0")
                        .param("size", "100"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].deviceId").value("123"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].status").value("online"));

        Mockito.verify(deviceStatusService, Mockito.times(1))
                .getDeviceStatus(startDate, endDate, 0, 100);
    }

    @Test
    void testGetDeviceStatus_InvalidToken() throws Exception {
        Mockito.when(jwtTokenService.needsAuthentication(Mockito.any(HttpServletRequest.class)))
                .thenReturn(true);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/device-status")
                        .param("startDate", startDate)
                        .param("endDate", endDate))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(AuthDefine.InvalidToken.getDescription()));

        Mockito.verify(deviceStatusService, Mockito.never()).getDeviceStatus(Mockito.anyString(), Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt());
    }

    @Test
    void testGetDeviceStatus_ServiceThrowsException() throws Exception {
        Mockito.when(jwtTokenService.needsAuthentication(Mockito.any(HttpServletRequest.class)))
                .thenReturn(false);
        Mockito.when(deviceStatusService.getDeviceStatus(startDate, endDate, 0, 100))
                .thenThrow(new CustomException("Service error", HttpStatus.INTERNAL_SERVER_ERROR));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/device-status")
                        .param("startDate", startDate)
                        .param("endDate", endDate))
                .andExpect(MockMvcResultMatchers.status().isInternalServerError())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Service error"));
    }
}
