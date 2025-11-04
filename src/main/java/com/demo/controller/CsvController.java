package com.demo.controller;

import com.demo.enums.AuthDefine;
import com.demo.exception.CustomException;
import com.demo.service.CsvBatchService;
import com.demo.service.JwtTokenService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
@Tag(name = "Csv", description = "CSV File Upload and Processing")
public class CsvController {
    @Autowired
    private CsvBatchService csvBatchService;

    @Autowired
    private JwtTokenService jwtTokenService;

    @PostMapping(value = "/v1/csv/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Object> uploadCsv(
            @RequestParam(name = "files") MultipartFile file, HttpServletRequest request) throws Exception {
        if (jwtTokenService.needsAuthentication(request)) {
            throw new CustomException(AuthDefine.InvalidToken.getDescription(), HttpStatus.UNAUTHORIZED);
        }

        csvBatchService.processCsv(file);
        return ResponseEntity.ok("File uploaded!");
    }
}
