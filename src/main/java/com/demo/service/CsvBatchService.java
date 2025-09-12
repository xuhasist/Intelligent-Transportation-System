package com.demo.service;

import com.demo.exception.CustomException;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.stream.Collectors;

@Service
public class CsvBatchService {
    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private Job importJob;

    public void processCsv(MultipartFile file) throws Exception {
        if (file.isEmpty()) {
            throw new CustomException("File is empty", HttpStatus.BAD_REQUEST);
        }

        // if originalFilename is null, use a default name with timestamp
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            originalFilename = "uploaded_file_" + System.currentTimeMillis() + ".csv";
        }

        // define a temp directory for uploads
        Path tempFilePath = Paths.get("temp_uploads", originalFilename);
        Files.createDirectories(tempFilePath.getParent());

        // copy file stream to tempFilePath
        try (InputStream is = file.getInputStream()) {
            Files.copy(is, tempFilePath, StandardCopyOption.REPLACE_EXISTING);
        }

        // pass absolute file path as a simple string JobParameter
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("filePath", tempFilePath.toAbsolutePath().toString())
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        JobExecution jobExecution = jobLauncher.run(importJob, jobParameters);

        // wait for job to finish
        while (jobExecution.isRunning()) {
            Thread.sleep(100);
        }

        if (jobExecution.getStatus() != BatchStatus.COMPLETED) {
            String error = jobExecution.getAllFailureExceptions()
                    .stream()
                    .map(Throwable::getMessage)
                    .collect(Collectors.joining(", "));
            throw new CustomException("Batch job failed: " + error, HttpStatus.BAD_REQUEST);
        }
    }
}
