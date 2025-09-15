package com.demo.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
public class DeviceStatusServiceTest {

    @Mock
    private JdbcTemplate statusJdbcTemplate;

    @InjectMocks
    DeviceStatusService deviceStatusService;

    @Test
    void testGetDeviceStatus_singlePage_singleDay() throws Exception {
        String startDate = "2025-08-28";
        String endDate = "2025-08-28";
        int page = 0, size = 2;

        Map<String, Object> row1 = Map.of("id", 1, "status", "active");
        Map<String, Object> row2 = Map.of("id", 2, "status", "inactive");
        Map<String, Object> row3 = Map.of("id", 3, "status", "active");

        List<Map<String, Object>> results = List.of(row1, row2, row3);

        String sql = "SELECT * FROM dynamic_control_device_status_record_20250828";
        Mockito.when(statusJdbcTemplate.queryForList(sql)).thenReturn(results);

        Page<Map<String, Object>> pageResult = deviceStatusService.getDeviceStatus(startDate, endDate, page, size);

        Assertions.assertEquals(2, pageResult.getContent().size());
        Assertions.assertEquals(1, pageResult.getContent().get(0).get("id"));
        Assertions.assertEquals("inactive", pageResult.getContent().get(1).get("status"));

        Mockito.verify(statusJdbcTemplate, Mockito.times(1)).queryForList(sql);
    }

    @Test
    void testGetDeviceStatus_multiplePages_multipleDays() throws Exception {
        String startDate = "2025-08-28";
        String endDate = "2025-08-29";
        int page = 1, size = 2;

        Map<String, Object> row1 = Map.of("id", 1, "status", "active");
        Map<String, Object> row2 = Map.of("id", 2, "status", "inactive");
        Map<String, Object> row3 = Map.of("id", 3, "status", "active");
        Map<String, Object> row4 = Map.of("id", 4, "status", "inactive");

        String sqlDay1 = "SELECT * FROM dynamic_control_device_status_record_20250828";
        Mockito.when(statusJdbcTemplate.queryForList(sqlDay1)).thenReturn(List.of(row1, row2));

        String sqlDay2 = "SELECT * FROM dynamic_control_device_status_record_20250829";
        Mockito.when(statusJdbcTemplate.queryForList(sqlDay2)).thenReturn(List.of(row3, row4));

        Page<Map<String, Object>> pageResult = deviceStatusService.getDeviceStatus(startDate, endDate, page, size);

        Assertions.assertEquals(2, pageResult.getContent().size());
        Assertions.assertEquals(3, pageResult.getContent().get(0).get("id"));
        Assertions.assertEquals("inactive", pageResult.getContent().get(1).get("status"));

        Mockito.verify(statusJdbcTemplate, Mockito.times(1)).queryForList(sqlDay1);
        Mockito.verify(statusJdbcTemplate, Mockito.times(1)).queryForList(sqlDay2);
    }

    @Test
    void testGetDeviceStatus_tableNotExist_throwsException() {
        String startDate = "2025-08-28";
        String endDate = "2025-08-28";

        Mockito.when(statusJdbcTemplate.queryForList("SELECT * FROM dynamic_control_device_status_record_20250828"))
                .thenThrow(new RuntimeException("Table does not exist"));

        Exception exception = Assertions.assertThrows(Exception.class, () ->
                deviceStatusService.getDeviceStatus(startDate, endDate, 0, 10));

        Assertions.assertEquals("Error querying device status records.",
                exception.getMessage());
    }
}
