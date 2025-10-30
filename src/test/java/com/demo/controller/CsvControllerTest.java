package com.demo.controller;

import com.demo.enums.AuthDefine;
import com.demo.exception.CustomException;
import com.demo.exception.GlobalExceptionHandler;
import com.demo.service.CsvBatchService;
import com.demo.service.JwtTokenService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MultipartFile;

@AutoConfigureMockMvc(addFilters = false)
public class CsvControllerTest {
    @Mock
    private CsvBatchService csvBatchService;

    @Mock
    private JwtTokenService jwtTokenService;

    @InjectMocks
    private CsvController csvController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders
                .standaloneSetup(csvController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testUploadCsv_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "test.csv",
                "text/csv",
                "id,name\n1,John".getBytes()
        );

        // JWT valid
        Mockito.when(jwtTokenService.needsAuthentication(Mockito.any(HttpServletRequest.class)))
                .thenReturn(false);

        // simulate processCsv does nothing
        Mockito.doNothing().when(csvBatchService).processCsv(Mockito.any(MultipartFile.class));

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/csv/upload")
                        .file(file))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().string("File uploaded!"));

        Mockito.verify(jwtTokenService, Mockito.times(1))
                .needsAuthentication(Mockito.any(HttpServletRequest.class));
        Mockito.verify(csvBatchService, Mockito.times(1))
                .processCsv(Mockito.any(MultipartFile.class));
    }

    @Test
    void testUploadCsv_InvalidToken() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "test.csv",
                "text/csv",
                "id,name\n1,John".getBytes()
        );

        // JWT invalid
        Mockito.when(jwtTokenService.needsAuthentication(Mockito.any(HttpServletRequest.class)))
                .thenReturn(true);

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/csv/upload")
                        .file(file))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(AuthDefine.InvalidToken.getDescription()));

        Mockito.verify(csvBatchService, Mockito.never()).processCsv(Mockito.any());
    }

    @Test
    void testUploadCsv_ProcessCsvThrowsException() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "test.csv",
                "text/csv",
                "id,name\n1,John".getBytes()
        );

        Mockito.when(jwtTokenService.needsAuthentication(Mockito.any(HttpServletRequest.class)))
                .thenReturn(false);

        // simulate csvBatchService.processCsv throws CustomException
        Mockito.doThrow(new CustomException("CSV parse error", HttpStatus.BAD_REQUEST))
                .when(csvBatchService).processCsv(Mockito.any(MultipartFile.class));

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/csv/upload")
                        .file(file))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("CSV parse error"));
    }
}
