package com.demo.itsproject;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@SpringBootTest
@ActiveProfiles("test")  // use application-test.properties
class ItsprojectApplicationTests {
    private static final Logger log = LoggerFactory.getLogger(ItsprojectApplicationTests.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");

    @Test
    void contextLoads() {
        ZonedDateTime currentTime = ZonedDateTime.now();
        String notify = "Test started successfully at " + currentTime.format(formatter);
        log.info(notify);
    }
}
