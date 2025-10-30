package com.demo.service;

import com.demo.dto.ThresholdDto;
import com.demo.exception.CustomException;
import com.demo.model.dynamic.*;
import com.demo.repository.dynamic.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DynamicServiceTest {
    @InjectMocks
    private DynamicService dynamicService;

    @Mock
    private CctvCarflowInstantRepository cctvCarflowInstantRepository;

    @Mock
    private DynamicThresholdRepository dynamicThresholdRepository;

    @Mock
    private DynamicLogRepository dynamicLogRepository;

    @Mock
    private DynamicConditionRepository dynamicConditionRepository;

    @Mock
    private DynamicPlanidRepository dynamicPlanidRepository;


    @Test
    void testGetTotalCarFlow() {
        String cctvId = "CCTV-412";
        LocalDateTime endTime = LocalDateTime.now();
        int interval = 5;

        // Mock the repository return value
        when(cctvCarflowInstantRepository.findCarflowSumByCctvIdAndEndTime(cctvId, endTime, interval))
                .thenReturn(150.0);

        // Call the service method
        double result = dynamicService.getTotalCarFlow(cctvId, endTime, interval);

        // Assert the result
        assertEquals(150.0, result);

        // Verify that the repository method was called once
        verify(cctvCarflowInstantRepository, times(1))
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
        when(cctvCarflowInstantRepository.findCarflowSumByCctvIdAndEndTimeAndStartPositionAndEndPosition(
                cctvId, endTime, interval, startPos, endPos))
                .thenReturn(45.0);

        // Call the service method
        double result = dynamicService.getSegmentCarFlow(cctvId, endTime, interval, startPos, endPos);

        // Assert the result
        assertEquals(45.0, result);

        // Verify that the repository method was called once
        verify(cctvCarflowInstantRepository, times(1))
                .findCarflowSumByCctvIdAndEndTimeAndStartPositionAndEndPosition(cctvId, endTime, interval, startPos, endPos);
    }

    @Test
    void testCreateConditionMap() {
        DynamicCondition condition1 = new DynamicCondition();
        condition1.setProgramId("prog1");
        condition1.setConditionExpression("expr1");
        condition1.setConsecutiveMatches(3);

        DynamicCondition condition2 = new DynamicCondition();
        condition2.setProgramId("prog2");
        condition2.setConditionExpression("expr2");
        condition2.setConsecutiveMatches(5);

        dynamicService.createConditionMap(List.of(condition1, condition2));

        assertEquals(2, dynamicService.getConditionMap().size());
        assertTrue(dynamicService.getConditionMap().containsKey("prog1"));
        assertEquals("expr1", dynamicService.getConditionMap().get("prog1").getConditionExpression());
    }

    @Test
    void testCreateTrafficAndThresholdMap() {
        DynamicThreshold threshold = new DynamicThreshold();
        threshold.setTimeLabel("平日0600-0630,假日1200-1230");
        threshold.setCctvId("CCTV-1,CCTV-2");
        threshold.setCarflowDirection("A-D,C-B");
        threshold.setTimeInterval(5);
        threshold.setComparisonOperator(">");
        threshold.setThresholdValue(100);
        threshold.setId(DynamicThresholdId.builder()
                .programId("prog1")
                .subId(1)
                .build());

        dynamicService.createTrafficAndThresholdMap(List.of(threshold));

        // Boolean: weekday:1 or weekend:0
        assertTrue(dynamicService.getTrafficPeriodsMap().containsKey(true));  // weekday
        assertTrue(dynamicService.getTrafficPeriodsMap().containsKey(false)); // weekend

        String key = "prog1-1";
        assertTrue(dynamicService.getThresholdMap().containsKey(key));
        ThresholdDto dto = dynamicService.getThresholdMap().get(key);
        assertEquals(List.of("CCTV-1", "CCTV-2"), dto.getCctvList());
        assertEquals(List.of("A-D", "C-B"), dto.getCarflowDirectionList());
    }

    @Test
    void testUpdateDynamicThreshold_existing() {
        DynamicThreshold existing = new DynamicThreshold();
        existing.setId(new DynamicThresholdId("prog1", 1));
        existing.setTimeLabel("0600-0630");

        DynamicThreshold update = new DynamicThreshold();
        update.setId(new DynamicThresholdId("prog1", 1));
        update.setTimeLabel("0700-0730");

        when(dynamicThresholdRepository.findById(new DynamicThresholdId("prog1", 1))).thenReturn(Optional.of(existing));
        when(dynamicThresholdRepository.save(existing)).thenReturn(existing);

        DynamicThreshold result = dynamicService.updateDynamicThreshold(update);
        assertEquals("0700-0730", result.getTimeLabel());
    }

    @Test
    void testIsInTrafficPeriod_normal() {
        LocalTime now = LocalTime.now();
        LocalTime start = now.minusMinutes(1);
        LocalTime end = now.plusMinutes(1);

        assertTrue(dynamicService.isInTrafficPeriod(start, end));
    }

    @Test
    void testSaveDynamicLog() {
        dynamicService.saveDynamicLog("prog1", "TestDevice", 41, "Y", "apply dynamic control for TC");

        verify(dynamicLogRepository, times(1))
                .save(Mockito.any(DynamicLog.class));
    }

    @Test
    void testGetAllDynamicConditions() {
        List<DynamicCondition> mockList = List.of(new DynamicCondition(), new DynamicCondition());
        when(dynamicConditionRepository.findAll()).thenReturn(mockList);

        List<DynamicCondition> result = dynamicService.getAllDynamicConditions();

        assertEquals(2, result.size());
        verify(dynamicConditionRepository, times(1)).findAll();
    }

    @Test
    void testGetAllDynamicThresholds() {
        List<DynamicThreshold> mockList = List.of(new DynamicThreshold());
        when(dynamicThresholdRepository.findAll()).thenReturn(mockList);

        List<DynamicThreshold> result = dynamicService.getAllDynamicThresholds();

        assertEquals(1, result.size());
        verify(dynamicThresholdRepository, times(1)).findAll();
    }

    @Test
    void testGetAllDynamicPlanIds_weekdayAndHoliday() {
        DynamicPlanid plan1 = new DynamicPlanid();
        plan1.setTcId("TC001");
        plan1.setPlanId(10);
        plan1.setTime("0000-2359");

        when(dynamicPlanidRepository.findByProgramIdAndDay("P1", "平日"))
                .thenReturn(List.of(plan1));
        when(dynamicPlanidRepository.findByProgramIdAndDay("P1", "假日"))
                .thenReturn(List.of(plan1));

        // weekday
        Map<String, Integer> weekdayMap = dynamicService.getAllDynamicPlanIds("P1", true);
        assertTrue(weekdayMap.containsKey("TC001"));

        // holiday
        Map<String, Integer> holidayMap = dynamicService.getAllDynamicPlanIds("P1", false);
        assertTrue(holidayMap.containsKey("TC001"));

        verify(dynamicPlanidRepository, times(1)).findByProgramIdAndDay("P1", "平日");
        verify(dynamicPlanidRepository, times(1)).findByProgramIdAndDay("P1", "假日");
    }

    @Test
    void testUpdateDynamicThreshold_success() {
        DynamicThresholdId id = new DynamicThresholdId("P1", 1);
        DynamicThreshold oldEntity = new DynamicThreshold();
        oldEntity.setId(id);
        oldEntity.setTimeLabel("Old");

        DynamicThreshold newEntity = new DynamicThreshold();
        newEntity.setId(id);
        newEntity.setTimeLabel("New");

        when(dynamicThresholdRepository.findById(id)).thenReturn(Optional.of(oldEntity));
        when(dynamicThresholdRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        DynamicThreshold result = dynamicService.updateDynamicThreshold(newEntity);

        assertEquals("New", result.getTimeLabel());
        verify(dynamicThresholdRepository).findById(id);
        verify(dynamicThresholdRepository).save(any(DynamicThreshold.class));
    }

    @Test
    void testUpdateDynamicThreshold_notFound() {
        DynamicThresholdId id = new DynamicThresholdId("P1", 99);
        DynamicThreshold newEntity = new DynamicThreshold();
        newEntity.setId(id);

        when(dynamicThresholdRepository.findById(id)).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class, () -> dynamicService.updateDynamicThreshold(newEntity));
        assertEquals("Data not found.", ex.getMessage());
    }

    @Test
    void testUpdateDynamicThreshold_nullInput() {
        assertNull(dynamicService.updateDynamicThreshold(null));

        DynamicThreshold t = new DynamicThreshold();
        assertNull(dynamicService.updateDynamicThreshold(t));
    }
}
