package com.demo.controller;

import com.demo.model.its.Users;
import com.demo.dto.ApiResponse;
import com.demo.dto.JwtAuthenticationResponse;
import com.demo.dto.SignupRequest;
import com.demo.dto.SystemLogResponse;
import com.demo.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    private AuthService authService;

    @PostMapping("/signin")
    public ResponseEntity<Object> authenticateUser(@Valid @RequestBody Users loginRequest) {
        JwtAuthenticationResponse response = authService.authenticate(loginRequest.getUsername(), loginRequest.getPassword());
        SystemLogResponse<JwtAuthenticationResponse> retVal = new SystemLogResponse<>();
        retVal.setData(response);

        return ResponseEntity.ok(retVal);
    }

    @PostMapping("/signup")
    public ResponseEntity<Object> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        URI location = authService.register(signUpRequest);
        return ResponseEntity.created(location).body(new ApiResponse(true, "signup successful"));
    }
}
