package com.demo.itsproject;

import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;


@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ItsprojectApplicationTests {
    private static final Logger log = LoggerFactory.getLogger(ItsprojectApplicationTests.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");

    @Test
    @Order(1)
    void contextLoads() {
        ZonedDateTime currentTime = ZonedDateTime.now();
        Assertions.assertNotNull(currentTime, "Current time should not be null");
        String notify = "Test started successfully at " + currentTime.format(formatter);
        log.info(notify);
    }
}
