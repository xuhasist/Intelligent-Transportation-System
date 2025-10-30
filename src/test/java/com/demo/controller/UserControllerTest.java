package com.demo.controller;

import com.demo.dto.UserDto;
import com.demo.service.UserService;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import com.demo.exception.GlobalExceptionHandler;
import com.demo.service.JwtTokenService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@AutoConfigureMockMvc(addFilters = false)
public class UserControllerTest {
    private MockMvc mockMvc;

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler()) // 若你有全域 ExceptionHandler
                .build();
    }

    @Test
    void testGetUserInfo_Success() throws Exception {
        when(jwtTokenService.needsAuthentication(any(HttpServletRequest.class))).thenReturn(false);

        UserDto user1 = UserDto.builder().username("Alice").build();
        UserDto user2 = UserDto.builder().username("Bob").build();

        when(userService.getUserInfo()).thenReturn(List.of(user1, user2));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/user/getUserInfo"))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].username").value("Alice"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].username").value("Bob"));

        verify(jwtTokenService, times(1)).needsAuthentication(any(HttpServletRequest.class));
        verify(userService, times(1)).getUserInfo();
    }

    @Test
    void testGetUserInfo_InvalidToken() throws Exception {
        when(jwtTokenService.needsAuthentication(any(HttpServletRequest.class))).thenReturn(true);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/user/getUserInfo"))
                .andExpect(status().isUnauthorized());

        verify(jwtTokenService, times(1)).needsAuthentication(any(HttpServletRequest.class));
        verify(userService, never()).getUserInfo();
    }
}
