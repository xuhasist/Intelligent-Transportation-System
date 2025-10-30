package com.demo.controller;

import com.demo.exception.GlobalExceptionHandler;
import com.demo.model.dynamic.DynamicThreshold;
import com.demo.model.dynamic.DynamicThresholdId;
import com.demo.service.DynamicService;
import com.demo.service.JwtTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.*;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@AutoConfigureMockMvc(addFilters = false)
public class DynamicControllerTest {
    @InjectMocks
    private DynamicController dynamicController;

    @Mock
    private DynamicService dynamicService;

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private HttpServletRequest request;

    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    private DynamicThreshold t1;
    private DynamicThreshold t2;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(dynamicController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        DynamicThresholdId id1 = DynamicThresholdId.builder()
                .programId("p1")
                .subId(1)
                .build();

        t1 = DynamicThreshold.builder()
                .id(id1)
                .timeInterval(60)
                .carflowDirection("NORTH")
                .comparisonOperator(">")
                .thresholdValue(100)
                .build();

        DynamicThresholdId id2 = DynamicThresholdId.builder()
                .programId("p2")
                .subId(2)
                .build();

        t2 = DynamicThreshold.builder()
                .id(id2)
                .timeInterval(60)
                .carflowDirection("SOUTH")
                .comparisonOperator("<")
                .thresholdValue(50)
                .build();
    }

    @Test
    void testGetAllDynamicThreshold_Unauthorized() throws Exception {
        when(jwtTokenService.needsAuthentication(any(HttpServletRequest.class))).thenReturn(true);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/dynamic/getAllDynamicThreshold"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testGetAllDynamicThreshold_Success() throws Exception {
        when(jwtTokenService.needsAuthentication(any(HttpServletRequest.class))).thenReturn(false);

        List<DynamicThreshold> thresholds = List.of(t1, t2);

        when(dynamicService.getAllDynamicThresholds()).thenReturn(thresholds);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/dynamic/getAllDynamicThreshold"))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].id.programId").value("p1"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].carflowDirection").value("NORTH"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].thresholdValue").value(100))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].id.programId").value("p2"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].comparisonOperator").value("<"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].thresholdValue").value(50));

        verify(dynamicService, times(1)).getAllDynamicThresholds();
    }

    @Test
    void testUpdateDynamicThreshold_Success() throws Exception {
        when(jwtTokenService.needsAuthentication(any(HttpServletRequest.class))).thenReturn(false);
        when(dynamicService.updateDynamicThreshold(any(DynamicThreshold.class))).thenReturn(t1);

        mockMvc.perform(MockMvcRequestBuilders.put("/api/dynamic/updateDynamicThreshold")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(t1)))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(t1)));

        verify(dynamicService, times(1)).updateDynamicThreshold(any(DynamicThreshold.class));
    }

    @Test
    void testUpdateDynamicThreshold_Unauthorized() throws Exception {
        when(jwtTokenService.needsAuthentication(any(HttpServletRequest.class))).thenReturn(true);

        mockMvc.perform(MockMvcRequestBuilders.put("/api/dynamic/updateDynamicThreshold")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(t1)))
                .andExpect(status().isUnauthorized());

        verify(dynamicService, never()).updateDynamicThreshold(any());
    }
}
