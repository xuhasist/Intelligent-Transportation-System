package com.demo.controller;

import com.demo.enums.ErrorDefine;
import com.demo.exception.CustomException;
import com.demo.service.DeviceStatusService;
import com.demo.service.JwtTokenService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/device-status")
public class DeviceStatusController {

    @Autowired
    private DeviceStatusService deviceStatusService;

    @Autowired
    private JwtTokenService jwtTokenService;

    @GetMapping
    public Page<Map<String, Object>> getDeviceStatus(
            HttpServletRequest request,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size
    ) throws Exception {
        if (jwtTokenService.needsAuthentication(request)) {
            throw new CustomException(ErrorDefine.InvalidToken.getDescription(), HttpStatus.UNAUTHORIZED);
        }

        return deviceStatusService.getDeviceStatus(startDate, endDate, page, size);
    }
}
