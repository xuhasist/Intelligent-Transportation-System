package com.demo.controller;

import com.demo.enums.ErrorDefine;
import com.demo.exception.CustomException;
import com.demo.service.JwtTokenService;
import com.demo.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/user")
public class UserController {
    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private UserService UserService;

    @GetMapping("/getUserInfo")
    public ResponseEntity<Object> getUserInfo(HttpServletRequest request) {
        if (jwtTokenService.needsAuthentication(request)) {
            throw new CustomException(ErrorDefine.InvalidToken.getDescription(), HttpStatus.UNAUTHORIZED);
        }

        return ResponseEntity.ok(UserService.getUserInfo());
    }
}
