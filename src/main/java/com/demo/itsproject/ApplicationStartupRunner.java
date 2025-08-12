package com.demo.itsproject;

import com.demo.service.MqttClientService;
import com.demo.service.SocketService;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class ApplicationStartupRunner implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(ApplicationStartupRunner.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");

    @Autowired
    private MqttClientService mqttClientService;

    @Autowired
    private SocketService socketService;

    @Override
    public void run(String... args) {
        try {
            ZonedDateTime currentTime = ZonedDateTime.now();
            log.info("System starting at {}", currentTime.format(formatter));

            mqttClientService.connect();
            socketService.socketConnect();
        } catch (Exception e) {
            log.error("Startup failed: ", e);
        }
    }
}
