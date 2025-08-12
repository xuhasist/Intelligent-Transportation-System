package com.demo.controller;

import com.demo.enums.ErrorDefine;
import com.demo.exception.CustomException;
import com.demo.model.dynamic.DynamicThreshold;
import com.demo.service.DynamicService;
import com.demo.service.JwtTokenService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dynamic")
public class DynamicController {

    @Autowired
    private DynamicService dynamicService;

    @Autowired
    private JwtTokenService jwtTokenService;

    @GetMapping("/getAllDynamicThreshold")
    public ResponseEntity<Object> getAllDynamicThreshold(HttpServletRequest request) {
        if (jwtTokenService.needsAuthentication(request)) {
            throw new CustomException(ErrorDefine.InvalidToken.getDescription(), HttpStatus.UNAUTHORIZED);
        }

        return ResponseEntity.ok(dynamicService.getAllDynamicThresholds());
    }

    @PutMapping("/updateDynamicThreshold")
    public ResponseEntity<Object> updateDynamicThreshold(
            HttpServletRequest request,
            @RequestBody DynamicThreshold updateData) {
        if (jwtTokenService.needsAuthentication(request)) {
            throw new CustomException(ErrorDefine.InvalidToken.getDescription(), HttpStatus.UNAUTHORIZED);
        }

        return ResponseEntity.ok(dynamicService.updateDynamicThreshold(updateData));
    }
}
