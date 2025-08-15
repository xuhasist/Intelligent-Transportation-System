package com.demo.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class DiscordNotifier {
    private static final Logger log = LoggerFactory.getLogger(DiscordNotifier.class);

    @Value("${discord.notify.token}")
    private String token;

    @Value("${discord.notify.url}")
    private String discordUrl;

    @Value("${discord.notify.channelId}")
    private String channelId;

    private final RestTemplate restTemplate = new RestTemplate();

    public void sendMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            log.warn("Message is null or empty.");
            return;
        }

        String url = discordUrl + channelId + "/messages";
        Map<String, String> payload = Map.of("content", message);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bot " + token);
        headers.set("User-Agent", "DiscordBot (https://yourwebsite.com, 1.0)");

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                log.warn("Failed to send Discord message. StatusCode: {}, Response: {}",
                        response.getStatusCode(), response.getBody());
            }
        } catch (Exception e) {
            log.error("Error sending Discord message", e);
        }
    }
}
