package com.demo.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import com.demo.exception.CustomException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.http.HttpStatus;


@ExtendWith(MockitoExtension.class)
public class CsvBatchServiceTest {
    @InjectMocks
    private CsvBatchService csvBatchService;

    @Mock
    private JobLauncher jobLauncher;

    @Mock
    private Job importJob;

    @Test
    void testProcessCsv_success() throws Exception {
        // mock MultipartFile
        MockMultipartFile file = new MockMultipartFile(
                "file",     // form field name
                "test.csv",       // original file name
                "text/csv",       // content type
                "a,b,c\n1,2,3".getBytes()   // file content
        );

        // simulate JobLancher behavior
        JobExecution mockExecution = mock(JobExecution.class);
        when(jobLauncher.run(any(Job.class), any(JobParameters.class))).thenReturn(mockExecution);
        when(mockExecution.isRunning()).thenReturn(false);
        when(mockExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);

        assertDoesNotThrow(() -> csvBatchService.processCsv(file));
        verify(jobLauncher, times(1)).run(eq(importJob), any(JobParameters.class));
    }

    @Test
    void testProcessCsv_emptyFile_throwsException() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.csv",
                "text/csv",
                new byte[0]
        );

        CustomException exception = assertThrows(CustomException.class, () -> {
            csvBatchService.processCsv(emptyFile);
        });

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("File is empty", exception.getMessage());
    }

    @Test
    void testProcessCsv_jobFails_throwsException() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.csv",
                "text/csv",
                "a,b,c\n1,2,3".getBytes()
        );

        JobExecution mockExecution = mock(JobExecution.class);
        when(jobLauncher.run(any(Job.class), any(JobParameters.class))).thenReturn(mockExecution);
        when(mockExecution.isRunning()).thenReturn(false);
        when(mockExecution.getStatus()).thenReturn(BatchStatus.FAILED);
        when(mockExecution.getAllFailureExceptions()).thenReturn(
                java.util.List.of(new RuntimeException("Some batch error"))
        );

        CustomException exception = assertThrows(CustomException.class, () -> {
            csvBatchService.processCsv(file);
        });

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertTrue(exception.getMessage().contains("Batch job failed: Some batch error"));
    }
}
