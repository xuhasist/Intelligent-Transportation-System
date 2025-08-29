package com.demo.itsproject;

import com.demo.notification.DiscordNotifier;
import com.demo.service.MqttClientService;
import com.demo.service.SocketService;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Component
@Profile("!test")
public class ApplicationStartupRunner implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(ApplicationStartupRunner.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");

    @Autowired
    private MqttClientService mqttClientService;

    @Autowired
    private SocketService socketService;

    @Autowired
    private DiscordNotifier discordNotifier;

    @Override
    public void run(String... args) {
        try {
            ZonedDateTime currentTime = ZonedDateTime.now();
            String notify = "System started successfully at " + currentTime.format(formatter);
            log.info(notify);
            discordNotifier.sendMessage(notify);

            mqttClientService.connect();
            socketService.socketConnect();
        } catch (Exception e) {
            log.error("Startup failed: ", e);
        }
    }
}
