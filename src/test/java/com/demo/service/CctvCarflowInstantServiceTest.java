package com.demo.service;

import com.demo.repository.dynamic.CctvCarflowInstantRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

@ExtendWith(MockitoExtension.class)
public class CctvCarflowInstantServiceTest {

    @Mock
    private CctvCarflowInstantRepository cctvCarflowInstantRepository;

    @InjectMocks
    private DynamicService dynamicService;

    @Test
    void testGetTotalCarFlow() {
        String cctvId = "CCTV-412";
        LocalDateTime endTime = LocalDateTime.now();
        int interval = 5;

        // Mock the repository return value
        Mockito.when(cctvCarflowInstantRepository.findCarflowSumByCctvIdAndEndTime(cctvId, endTime, interval))
                .thenReturn(150.0);

        // Call the service method
        double result = dynamicService.getTotalCarFlow(cctvId, endTime, interval);

        // Assert the result
        Assertions.assertEquals(150.0, result);

        // Verify that the repository method was called once
        Mockito.verify(cctvCarflowInstantRepository, Mockito.times(1))
                .findCarflowSumByCctvIdAndEndTime(cctvId, endTime, interval);
    }

    @Test
    void testGetSegmentCarFlow() {
        String cctvId = "CCTV-412";
        LocalDateTime endTime = LocalDateTime.now();
        int interval = 5;
        String startPos = "D";
        String endPos = "A";

        // Mock the repository return value
        Mockito.when(cctvCarflowInstantRepository.findCarflowSumByCctvIdAndEndTimeAndStartPositionAndEndPosition(
                        cctvId, endTime, interval, startPos, endPos))
                .thenReturn(45.0);

        // Call the service method
        double result = dynamicService.getSegmentCarFlow(cctvId, endTime, interval, startPos, endPos);

        // Assert the result
        Assertions.assertEquals(45.0, result);

        // Verify that the repository method was called once
        Mockito.verify(cctvCarflowInstantRepository, Mockito.times(1))
                .findCarflowSumByCctvIdAndEndTimeAndStartPositionAndEndPosition(cctvId, endTime, interval, startPos, endPos);
    }
}
