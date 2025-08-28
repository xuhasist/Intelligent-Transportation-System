package com.demo.controller;

import com.demo.dto.*;
import com.demo.enums.AuthDefine;
import com.demo.exception.CustomException;
import com.demo.model.its.User;
import com.demo.service.AuthService;
import com.demo.service.JwtTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    private AuthService authService;

    @Autowired
    private JwtTokenService jwtTokenService;

    @PostMapping("/signin")
    public ResponseEntity<Object> authenticateUser(@Valid @RequestBody User loginRequest) {
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

    @PostMapping("/changePassword")
    public ResponseEntity<Object> changePassword(HttpServletRequest request,
                                                 @Valid @RequestBody ChangePasswordRequest changePwdRequest) {
        if (jwtTokenService.needsAuthentication(request, true)) {
            throw new CustomException(AuthDefine.InvalidToken.getDescription(), HttpStatus.UNAUTHORIZED);
        }

        authService.changePassword(changePwdRequest);
        return ResponseEntity.ok(new ApiResponse(true, "Password changed successfully"));
    }
}
