package com.demo.controller;

import com.demo.enums.AuthDefine;
import com.demo.exception.CustomException;
import com.demo.dto.TcMessageLogDto;
import com.demo.service.JwtTokenService;
import com.demo.service.TcService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TcRestController {

    @Autowired
    private TcService tcService;

    @Autowired
    private JwtTokenService jwtTokenService;

    @GetMapping("/getTCMessageLog")
    public Page<TcMessageLogDto> getTCMessageLog(
            HttpServletRequest request,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size
    ) throws Exception {
        if (jwtTokenService.needsAuthentication(request)) {
            throw new CustomException(AuthDefine.InvalidToken.getDescription(), HttpStatus.UNAUTHORIZED);
        }

        return tcService.getTCMessageLog(startDate, endDate, page, size);
    }

    @GetMapping("/getTCInfo")
    public ResponseEntity<Object> getTCInfo(HttpServletRequest request) {
        if (jwtTokenService.needsAuthentication(request)) {
            throw new CustomException(AuthDefine.InvalidToken.getDescription(), HttpStatus.UNAUTHORIZED);
        }

        return ResponseEntity.ok(tcService.getTCInfo());
    }
}
