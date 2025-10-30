package com.demo.manager;

import com.demo.dto.ConditionDto;
import com.demo.dto.ThresholdDto;
import com.demo.dto.TrafficPeriodDto;
import com.demo.enums.ControlStrategy;
import com.demo.model.dynamic.DynamicParameters;
import com.demo.model.dynamic.DynamicParametersId;
import com.demo.model.its.TcInfo;
import com.demo.notification.DiscordNotifier;
import com.demo.repository.its.TcInfoRepository;
import com.demo.service.DynamicService;
import com.demo.service.SocketService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DynamicControlManagerTest {
    @InjectMocks
    private DynamicControlManager dynamicControlManager;

    @Mock
    private DynamicService dynamicService;

    @Mock
    private TcReceiveMessageManager tcReceiveMessageManager;

    @Mock
    private TcSendMessageManager tcSendMessageManager;

    @Mock
    private TcInfoRepository tcInfoRepository;

    @Mock
    private SocketService socketService;

    @Mock
    private DiscordNotifier discordNotifier;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(dynamicControlManager, "debugMode", false);
    }

    @Test
    void testEvaluateCondition() {
        assertTrue(DynamicControlManager.evaluateCondition("5 > 2"));
        assertFalse(DynamicControlManager.evaluateCondition("1 > 2"));
    }

    @Test
    void testCalculateTotalCarFlow_AllDirection() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        ThresholdDto threshold = ThresholdDto.builder()
                .cctvList(List.of("CCTV1"))
                .carflowDirectionList(List.of("ALL"))
                .build();

        when(dynamicService.getTotalCarFlow(anyString(), any(LocalDateTime.class), anyInt())).thenReturn(10.0);

        Method method = DynamicControlManager.class.getDeclaredMethod("calculateTotalCarFlow", ThresholdDto.class, int.class);
        method.setAccessible(true);
        double totalFlow = (double) method.invoke(dynamicControlManager, threshold, 5);
        assertEquals(10.0, totalFlow);
    }

    @Test
    void testDynamicTrigger_Success() throws Exception {
        String programId = "P1";
        LocalTime startTime = LocalTime.of(8, 0);
        LocalTime endTime = LocalTime.of(9, 0);
        boolean isWeekday = true;

        TcInfo tcInfo = TcInfo.builder()
                .tcId("TC001")
                .ip("127.0.0.1")
                .port(1)
                .enable((byte) 1)
                .build();

        DynamicParametersId id1 = DynamicParametersId.builder()
                .programId(programId)
                .deviceId("TC001")
                .planId(0)
                .subphaseId(1)
                .build();

        DynamicParameters entry1 = DynamicParameters.builder()
                .id(id1)
                .location("LOC1")
                .phaseOrder("1")
                .cycleTime(60)
                .offset(0)
                .green(30)
                .pedGreenFlash(5)
                .pedRed(25)
                .yellow(5)
                .allRed(2)
                .minGreen(10)
                .maxGreen(40)
                .direct(0)
                .logTime(LocalDateTime.now())
                .build();

        JSONObject value5FC4 = new JSONObject();
        value5FC4.put("planId", 0);
        value5FC4.put("subPhaseCount", 1);
        value5FC4.put("minGreen", new JSONArray(List.of(10)));
        value5FC4.put("maxGreen", new JSONArray(List.of(40)));
        value5FC4.put("yellow", new JSONArray(List.of(5)));
        value5FC4.put("allRed", new JSONArray(List.of(2)));
        value5FC4.put("pedGreenFlash", new JSONArray(List.of(5)));
        value5FC4.put("pedRed", new JSONArray(List.of(25)));

        JSONObject value5FC5 = new JSONObject();
        value5FC5.put("planId", 0);
        value5FC5.put("cycleTime", 60);
        value5FC5.put("direct", 0);
        value5FC5.put("phaseOrder", "1");
        value5FC5.put("subPhaseCount", 1);
        value5FC5.put("offset", 0);
        value5FC5.put("green", new JSONArray(List.of(30)));

        Map<String, Integer> planMap = Map.of("TC001", 1);
        when(dynamicService.getAllDynamicPlanIds(programId, isWeekday)).thenReturn(planMap);
        when(dynamicService.getEntriesByProgramIdAndDeviceIdAndPlanId(programId, "TC001", 1))
                .thenReturn(List.of(entry1));
        when(tcInfoRepository.findByTcId("TC001")).thenReturn(tcInfo);
        when(tcInfoRepository.findById("TC001")).thenReturn(Optional.of(tcInfo));
        when(socketService.isHostConnected(anyString())).thenReturn(true);

        // simulate sendMessage success
        when(tcSendMessageManager.handle5F10Message(any())).thenReturn(true);
        when(tcSendMessageManager.handle5F15Message(any())).thenReturn(true);
        when(tcSendMessageManager.handle5F18Message(any())).thenReturn(true);
        when(tcSendMessageManager.handle5F40Message(any())).thenReturn(true);
        when(tcSendMessageManager.handle5F45Message(any())).thenReturn(true);

        // simulate receiveMessage returns
        when(tcReceiveMessageManager.getValueMap5FC0()).thenReturn(new ConcurrentHashMap<>() {{
            put("TC001", new JSONObject().put("ControlStrategy", ControlStrategy.Dynamic.getCode())
                    .put("EffectTime", 5));
        }});

        Map<String, JSONObject> map5FC4 = new ConcurrentHashMap<>();
        map5FC4.put("TC001", value5FC4);

        Map<String, JSONObject> map5FC5 = new ConcurrentHashMap<>();
        map5FC5.put("TC001", value5FC5);

        when(tcReceiveMessageManager.getValueMap5FC4()).thenReturn(map5FC4);
        when(tcReceiveMessageManager.getValueMap5FC5()).thenReturn(map5FC5);

        dynamicControlManager.dynamicTrigger(programId, startTime, endTime, isWeekday);

        String notify = "Dynamic control applied successfully for TC " + tcInfo.getTcId() + " at ";
        verify(discordNotifier, atLeastOnce()).sendMessage(contains(notify));
    }

    @Test
    void testStartTrafficCalculation() throws Exception {
        // 使用 builder 初始化 TrafficPeriodDto
        TrafficPeriodDto period = TrafficPeriodDto.builder()
                .programId("21001")
                .subId(1)
                .startTime(LocalTime.now().minusMinutes(1))
                .endTime(LocalTime.now().plusMinutes(1))
                .inSchedule(new AtomicBoolean(true))
                .build();

        // ThresholdDto
        ThresholdDto threshold = ThresholdDto.builder()
                .timeInterval(1)
                .cctvList(List.of("CCTV1"))
                .carflowDirectionList(List.of("ALL"))
                .comparisonOperator(">")
                .thresholdValue(0)
                .isMatch(false)
                .build();

        Map<String, ThresholdDto> thresholdMap = new HashMap<>();
        thresholdMap.put("21001-1", threshold);

        // Mock dependencies
        when(dynamicService.getThresholdMap()).thenReturn(thresholdMap);
        when(dynamicService.getTotalCarFlow(any(), any(), anyInt())).thenReturn(10.0);
        when(dynamicService.isInTrafficPeriod(any(), any())).thenReturn(true);

        // spy 監控 dynamicControlManager
        DynamicControlManager spyManager = spy(dynamicControlManager);
        doNothing().when(spyManager).checkConditionMatch(any(), any(), any(), anyBoolean(), anyLong());

        spyManager.startTrafficCalculation(period, true);

        // 等待 scheduler 執行一次
        Thread.sleep(500);

        assertTrue(threshold.getIsMatch()); // totalCarFlow > thresholdValue => match
    }

    @Test
    void testCheckConditionMatch_trueBranch() throws Exception {
        String programId = "21001";
        ConditionDto condition = ConditionDto.builder()
                .conditionExpression("1 || 0")
                .consecutiveMatches(1)
                .consecutiveCounts(new AtomicInteger(0))
                .lastTriggeredTime(new AtomicLong(0))
                .build();

        ThresholdDto threshold = new ThresholdDto();
        threshold.setIsMatch(true);

        Map<String, ConditionDto> conditionMap = new HashMap<>();
        conditionMap.put(programId, condition);

        Map<String, ThresholdDto> thresholdMap = new HashMap<>();
        thresholdMap.put("21001-1", threshold);

        when(dynamicService.getConditionMap()).thenReturn(conditionMap);
        when(dynamicService.getThresholdMap()).thenReturn(thresholdMap);

        // Mock dynamicTrigger to avoid real execution
        DynamicControlManager spyManager = spy(dynamicControlManager);
        doNothing().when(spyManager).dynamicTrigger(anyString(), any(), any(), anyBoolean());

        spyManager.checkConditionMatch(programId, LocalTime.now(), LocalTime.now().plusMinutes(1), true, 0L);

        assertEquals(0, condition.getConsecutiveCounts().get()); // should reset after trigger
    }

    @Test
    void testCompareMethods_returnFalse() {
        StringBuilder errorLog = new StringBuilder();

        // compareInt false branch
        boolean intResult = dynamicControlManager.compareInt("testInt", 1, 2, errorLog);
        assertFalse(intResult);
        assertTrue(errorLog.toString().contains("testInt mismatch"));

        errorLog.setLength(0);

        // compareString false branch
        boolean strResult = dynamicControlManager.compareString("testStr", "abc", "xyz", errorLog);
        assertFalse(strResult);
        assertTrue(errorLog.toString().contains("testStr mismatch"));

        errorLog.setLength(0);

        // compareList false branch
        List<Integer> expected = List.of(1, 2, 3);
        JSONArray actual = new JSONArray(List.of(4, 5, 6));
        boolean listResult = dynamicControlManager.compareList(expected, actual, "testList", errorLog);
        assertFalse(listResult);
        assertTrue(errorLog.toString().contains("testList list mismatch"));
    }
}
