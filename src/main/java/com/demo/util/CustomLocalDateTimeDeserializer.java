package com.demo.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CustomLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {
    private static final Logger log = LoggerFactory.getLogger(CustomLocalDateTimeDeserializer.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public LocalDateTime deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
        String dateTimeString = parser.getText().strip();
        try {
            // String to LocalDateTime
            return LocalDateTime.parse(dateTimeString, formatter);
        } catch (Exception e) {
            log.warn("Failed to parse date: {}", dateTimeString, e);
            return null;
        }
    }
}
