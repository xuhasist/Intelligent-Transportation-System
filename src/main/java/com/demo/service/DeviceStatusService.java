package com.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class DeviceStatusService {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Autowired
    @Qualifier("statusJdbcTemplate")
    private JdbcTemplate statusJdbcTemplate;

    public Page<Map<String, Object>> getDeviceStatus(String startDate, String endDate, int page, int size) throws Exception {
        List<LocalDate> dateRange = getDateRange(startDate, endDate);
        List<Map<String, Object>> deviceStatusList = new ArrayList<>();

        int startIndex = page * size;
        int endIndex = startIndex + size;
        int currentCount = 0;

        for (LocalDate date : dateRange) {
            String tableName = "dynamic_control_device_status_record_" + date.format(formatter);
            String sql = "SELECT * FROM " + tableName;

            try {
                // String is column name, Object is column value
                List<Map<String, Object>> results = statusJdbcTemplate.queryForList(sql);
                for (Map<String, Object> row : results) {
                    if (currentCount >= startIndex && currentCount < endIndex) {
                        deviceStatusList.add(row);
                    }
                    currentCount++;

                    if (currentCount >= endIndex) {
                        break;
                    }
                }

                if (currentCount >= endIndex) {
                    break;
                }
            } catch (Exception e) {
                throw new Exception("Error querying table " + tableName + ": " + e.getMessage());
            }
        }

        return new PageImpl<>(deviceStatusList, PageRequest.of(page, size), currentCount);
    }

    private List<LocalDate> getDateRange(String startDate, String endDate) {
        List<LocalDate> dateRange = new ArrayList<>();
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);

        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            dateRange.add(date);
        }

        return dateRange;
    }
}
