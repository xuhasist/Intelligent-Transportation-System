package com.demo.controller;

import com.demo.dto.ChangePasswordRequest;
import com.demo.dto.JwtAuthenticationResponse;
import com.demo.dto.SignupRequest;
import com.demo.exception.CustomException;
import com.demo.exception.GlobalExceptionHandler;
import com.demo.model.its.User;
import com.demo.service.AuthService;
import com.demo.service.JwtTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.net.URI;

@AutoConfigureMockMvc(addFilters = false)
public class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @Mock
    private JwtTokenService jwtTokenService;

    @InjectMocks
    private AuthController authController;

    private final ObjectMapper objectMapper = new ObjectMapper(); // For converting objects to JSON

    private final String username = "yufanLiu";
    private final String password = "myPassword";
    private final String newPassword = "myNewPassword";


    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this); // Initialize the mocks
        mockMvc = MockMvcBuilders
                .standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler()) // Add global exception handler
                .build();
    }

    @Test
    void testAuthenticateUser_Success() throws Exception {
        User loginRequest = new User();
        loginRequest.setUsername(username);
        loginRequest.setPassword(password);

        // Fake authentication response
        JwtAuthenticationResponse fakeResponse = JwtAuthenticationResponse.builder()
                .username(username)
                .accessToken("fake-jwt-token")
                .message("Authentication Success")
                .status(200)
                .build();

        Mockito.when(authService.authenticate(username, password))
                .thenReturn(fakeResponse);

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(MockMvcResultMatchers.status().isOk()) // Expect HTTP 200 OK
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.username").value(username))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.accessToken").value(fakeResponse.getAccessToken()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.message").value(fakeResponse.getMessage()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.status").value(fakeResponse.getStatus()));

        Mockito.verify(authService, Mockito.times(1))
                .authenticate(username, password);
    }

    @Test
    void testAuthenticateUser_Failed() throws Exception {
        User loginRequest = new User();
        loginRequest.setUsername(username);
        loginRequest.setPassword(password);

        Mockito.when(authService.authenticate(username, password))
                .thenThrow(new CustomException("Authentication failed", HttpStatus.UNAUTHORIZED));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Authentication failed"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.httpStatus").value(HttpStatus.UNAUTHORIZED.name()));

        Mockito.verify(authService, Mockito.times(1))
                .authenticate(username, password);
    }

    @Test
    void testRegisterUser_Success() throws Exception {
        SignupRequest request = new SignupRequest();
        request.setUsername(username);
        request.setPassword(password);

        URI location = URI.create("/users/" + username);
        Mockito.when(authService.register(Mockito.any(SignupRequest.class)))
                .thenReturn(location);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.header().string("Location", location.toString()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("signup successful"));

        Mockito.verify(authService, Mockito.times(1))
                .register(Mockito.any(SignupRequest.class));
    }

    @Test
    void testRegisterUser_UsernameExists() throws Exception {
        SignupRequest request = new SignupRequest();
        request.setUsername(username);
        request.setPassword(password);

        Mockito.when(authService.register(Mockito.any(SignupRequest.class)))
                .thenThrow(new CustomException("Username already in use", HttpStatus.CONFLICT));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(MockMvcResultMatchers.status().isConflict())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Username already in use"));

        Mockito.verify(authService, Mockito.times(1))
                .register(Mockito.any(SignupRequest.class));
    }

    @Test
    void testChangePassword_Success() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setUsername(username);
        request.setOldPassword(password);
        request.setNewPassword(newPassword);

        // return false means token valid
        Mockito.when(jwtTokenService.needsAuthentication(Mockito.any(HttpServletRequest.class), Mockito.eq(true)))
                .thenReturn(false);

        // don't need return value, just verify the service is called
        Mockito.doNothing().when(authService).changePassword(Mockito.any(ChangePasswordRequest.class));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/changePassword")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Password changed successfully"));

        Mockito.verify(authService, Mockito.times(1))
                .changePassword(Mockito.any(ChangePasswordRequest.class));
    }

    @Test
    void testChangePassword_InvalidToken() throws Exception {
        // Arrange: invalid JWT token
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setUsername(username);
        request.setOldPassword(password);
        request.setNewPassword(newPassword);

        // return true (token invalid)
        Mockito.when(jwtTokenService.needsAuthentication(Mockito.any(HttpServletRequest.class), Mockito.eq(true)))
                .thenReturn(true);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/changePassword")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Invalid or expired token."));

        // Service should not be called
        Mockito.verify(authService, Mockito.never()).changePassword(Mockito.any(ChangePasswordRequest.class));
    }

    @Test
    void testChangePassword_WeakPassword() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setUsername(username);
        request.setOldPassword(password);
        request.setNewPassword(newPassword);

        Mockito.when(jwtTokenService.needsAuthentication(Mockito.any(HttpServletRequest.class), Mockito.eq(true)))
                .thenReturn(false);

        // doThrow for void methods
        Mockito.doThrow(new CustomException("Password not strong", HttpStatus.FORBIDDEN))
                .when(authService).changePassword(Mockito.any(ChangePasswordRequest.class));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/changePassword")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(MockMvcResultMatchers.status().isForbidden())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Password not strong"));

        Mockito.verify(authService, Mockito.times(1)).changePassword(Mockito.any(ChangePasswordRequest.class));
    }
}

