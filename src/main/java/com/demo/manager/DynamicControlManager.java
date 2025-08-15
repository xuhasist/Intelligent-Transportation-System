package com.demo.manager;

import com.demo.enums.ControlStrategy;
import com.demo.enums.DynamicStatus;
import com.demo.exception.DynamicException;
import com.demo.model.dynamic.DynamicParameters;
import com.demo.model.its.TCInfo;
import com.demo.dto.ConditionDto;
import com.demo.dto.ThresholdDto;
import com.demo.dto.TrafficPeriodDto;
import com.demo.notification.DiscordNotifier;
import com.demo.repository.its.TCInfoRepository;
import com.demo.service.DynamicService;
import com.demo.service.SocketService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class DynamicControlManager {
    private static final Logger log = LoggerFactory.getLogger(DynamicControlManager.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final int effectTime = 5;
    private static final int testTimes = 1;
    private static AtomicInteger testCnt = new AtomicInteger(0);

    @Value("${app.debug:false}")
    private boolean debugMode;

    @Autowired
    private TCReceiveMessageManager tcReceiveMessageManager;

    @Autowired
    private TCSendMessageManager tcSendMessageManager;

    @Autowired
    private DynamicService dynamicService;

    @Autowired
    private TCInfoRepository tcInfoRepository;

    @Autowired
    private SocketService socketService;

    @Autowired
    private DiscordNotifier discordNotifier;

    @Async
    public void startTrafficCalculation(TrafficPeriodDto period, Boolean isWeekday) {
        try {
            period.setInSchedule(true);

            String program_id = period.getProgramId();
            int sub_id = period.getSubId();

            ThresholdDto thresholdData = dynamicService.getThresholdMap().get(program_id + "-" + sub_id);
            int timeInterval_minute = thresholdData.getTimeInterval();

            LocalTime startTime = period.getStartTime();
            LocalTime endTime = period.getEndTime();

            if (dynamicService.isInTrafficPeriod(startTime, endTime)) {
                double totalCarFlow = 0.0;
                for (String cctv : thresholdData.getCctvList()) {
                    List<String> carflowDirection = thresholdData.getCarflowDirectionList();
                    for (String direction : carflowDirection) {
                        if (direction.equalsIgnoreCase("ALL")) {
                            totalCarFlow += dynamicService.calculateCarFlow(cctv, LocalDateTime.now(), timeInterval_minute);
                        } else {
                            String[] pos = direction.split("-");  // e.g. "A-B"
                            String startPos = pos[0];
                            String endPos = pos[1];

                            totalCarFlow += dynamicService.calculateCarFlow(cctv, LocalDateTime.now(), timeInterval_minute, startPos, endPos);
                        }
                    }
                }

                // e.g. "180.0 > 55", check if totalCarFlow match condition
                String express = totalCarFlow + thresholdData.getComparisonOperator() + thresholdData.getThresholdValue();

                if (evaluateCondition(express)) {
                    thresholdData.setIsMatch(true);
                }

                // Check if all sub-conditions are satisfied for the final result
                checkConditionMatch(program_id, startTime, endTime, isWeekday);
            }
        } catch (Exception e) {
            log.error("Error in startTrafficCalculation: {}", e.getMessage());
        }
    }

    public static boolean evaluateCondition(String expression) {
        ExpressionParser parser = new SpelExpressionParser();
        Expression exp = parser.parseExpression(expression);
        return Boolean.TRUE.equals(exp.getValue(Boolean.class));
    }

    public void checkConditionMatch(String program_id, LocalTime startTime, LocalTime endTime, boolean isWeekday) {
        try {
            ConditionDto condition = dynamicService.getConditionMap().get(program_id);

            // 1. replace || -> or, && -> and, e.g. (1 and (2 or 3))
            String spelCondition = (condition.getConditionExpression())
                    .replace("||", " or ").replace("&&", " and ");

            Set<Map.Entry<String, ThresholdDto>> result = dynamicService.getThresholdMap().entrySet().stream()
                    .filter(entry -> entry.getKey().startsWith(program_id))
                    .collect(Collectors.toSet());

            // 2. replace number with true/false, e.g. (true and (false or false))
            for (Map.Entry<String, ThresholdDto> entry : result) {
                spelCondition = spelCondition.replaceAll("\\b" + entry.getKey().split("-")[1] + "\\b", String.valueOf(entry.getValue().getIsMatch()));
            }

            // 3. use SpEL parsing
            if (evaluateCondition(spelCondition)) {
                condition.setConsecutiveCounts(condition.getConsecutiveCounts() + 1);

                if (condition.getConsecutiveCounts().equals(condition.getConsecutiveMatches())) {
                    // apply dynamic control
                    dynamicTrigger(program_id, startTime, endTime, isWeekday);
                    condition.setConsecutiveCounts(0);    // reset after triggering
                }
            } else {
                condition.setConsecutiveCounts(0);    // reset, not consecutive match
            }

            if (debugMode && program_id.equals("21001")) {
                int current = testCnt.incrementAndGet();
                if (current <= testTimes) {
                    dynamicTrigger(program_id, startTime, endTime, isWeekday);
                }
            }
        } catch (Exception e) {
            log.error("Error in checkConditionMatch: {}", e.getMessage());
        }
    }

    public void dynamicTrigger(String program_id, LocalTime startTime, LocalTime endTime, boolean isWeekday) {
        try {
            Map<String, Integer> tcPlanMap = dynamicService.getAllDynamicPlanIds(program_id, isWeekday);

            for (String tc : tcPlanMap.keySet()) {
                int targetPlanId = tcPlanMap.get(tc);

                if (debugMode) {
                    tc = "TestDevice";  // for testing purposes, use a test device
                }

                String host = tcInfoRepository.findByTcId(tc).getIp();
                if (!socketService.isHostConnected(host)) {
                    dynamicService.saveDynamicLog(program_id, tc, targetPlanId, DynamicStatus.FAILURE.getCode(), "socket is not connected");
                    continue;
                }

                Optional<TCInfo> tcOpt = tcInfoRepository.findById(tc);
                TCInfo tcData = tcOpt.orElse(null);
                if (tcData == null || tcData.getEnable().equals(Byte.valueOf("0"))) {
                    dynamicService.saveDynamicLog(program_id, tc, targetPlanId, DynamicStatus.FAILURE.getCode(), "dynamic control not enabled for TC");
                    continue;
                }

                int retryCnt = 0;
                int maxRetry = 3; // maximum retry attempts
                boolean success = false;

                while (!success && retryCnt < maxRetry) {
                    try {
                        triggerDynamicControl(program_id, tc, targetPlanId);
                        dynamicService.saveDynamicLog(program_id, tc, targetPlanId, DynamicStatus.SUCCESS.getCode(), "apply dynamic control success");

                        String notify = "Dynamic control applied successfully for TC " + tc + " at " + LocalDateTime.now().format(formatter);
                        discordNotifier.sendMessage(notify);

                        success = true;
                        break;
                    } catch (DynamicException e) {
                        dynamicService.saveDynamicLog(program_id, tc, targetPlanId, DynamicStatus.FAILURE.getCode(), "apply dynamic control failed: " + e.getMessage());
                    }
                    retryCnt++;
                }

                if (!success) {
                    try {
                        tryCloseDynamic(tc);    // apply dynamic control failed, close it
                        dynamicService.saveDynamicLog(program_id, tc, targetPlanId, DynamicStatus.SUCCESS.getCode(), "5F10 disable dynamic control success");
                    } catch (DynamicException e) {
                        dynamicService.saveDynamicLog(program_id, tc, targetPlanId, DynamicStatus.FAILURE.getCode(), "5F10 disable dynamic control failed: " + e.getMessage());
                    }
                }

                Thread.sleep(100); // avoid too frequent requests
            }
        } catch (Exception e) {
            log.error("Error in dynamicTrigger: {}", e.getMessage());
        }
    }

    private void triggerDynamicControl(String programId, String tcId, int targetPlanId) {
        dynamicService.saveDynamicLog(programId, tcId, targetPlanId, DynamicStatus.SUCCESS.getCode(), "apply dynamic control for TC");

        try {
            /* Apply a sequence of commands to TC devices to trigger dynamic control */
            send5F10(tcId, ControlStrategy.Dynamic.getCode());  // enable dynamic control
            send5F40(tcId, ControlStrategy.Dynamic.getCode());  // check if dynamic control is set correctly

            List<DynamicParameters> data = dynamicService.getEntriesByProgramIdAndDeviceIdAndPlanId(programId, tcId, targetPlanId);
            if (data.isEmpty()) {
                throw new DynamicException("Dynamic parameters not found for programId: " + programId + ", tcId: " + tcId + ", targetPlanId: " + targetPlanId);
            }

            targetPlanId = 0;   // dynamic control can only be applied to plan ID 0

            send5F15(tcId, targetPlanId, data);      // set target plan ID and relevant parameters
            send5F45(tcId, targetPlanId, data);      // check if parameters is set correctly
            send5F18(tcId, targetPlanId);            // enable target plan ID
        } catch (Exception e) {
            throw new DynamicException(e.getMessage());
        }
    }

    private void tryCloseDynamic(String tcId) {
        try {
            send5F10(tcId, ControlStrategy.TOD.getCode());   // disable dynamic control, switch to TOD(Time-of-Day) strategy
        } catch (Exception e) {
            throw new DynamicException(e.getMessage());
        }
    }

    private void send5F10(String deviceId, int controlStrategy) throws InterruptedException {
        JSONObject value = new JSONObject();
        value.put("deviceId", deviceId);
        value.put("controlStrategy", controlStrategy);
        value.put("effectTime", effectTime);

        JSONObject obj = new JSONObject();
        obj.put("value", value);
        if (!tcSendMessageManager.handle5F10Message(obj)) {
            throw new DynamicException("5F10 dynamic control setting failed");
        }

        Thread.sleep(100);
    }

    private void send5F40(String deviceId, int controlStrategy) throws InterruptedException {
        JSONObject value = new JSONObject();
        value.put("deviceId", deviceId);

        JSONObject obj = new JSONObject();
        obj.put("value", value);

        if (!tcSendMessageManager.handle5F40Message(obj)) {
            throw new DynamicException("5F40 dynamic control check failed");
        } else {
            // check 5FC0
            long startTime = System.currentTimeMillis();
            JSONObject value5FC0;
            do {
                value5FC0 = tcReceiveMessageManager.getValueMap5FC0().get(deviceId);
            } while (value5FC0 == null && (System.currentTimeMillis() - startTime < 16000));    // wait for 16 seconds

            if (value5FC0 == null) {
                throw new DynamicException("5FC0 null failed");
            } else if (value5FC0.getInt("ControlStrategy") != controlStrategy || value5FC0.getInt("EffectTime") != effectTime) {
                throw new DynamicException("5FC0 parameter mismatch failed");
            }
            tcReceiveMessageManager.getValueMap5FC0().remove(deviceId);
        }

        Thread.sleep(100);
    }

    private void send5F15(String deviceId, int targetPlanId, List<DynamicParameters> data) throws InterruptedException {
        DynamicParameters first = data.getFirst();  // get the first entry to extract common parameters

        JSONObject value = new JSONObject();
        value.put("deviceId", deviceId);
        value.put("planId", targetPlanId);
        value.put("direct", first.getDirect());
        value.put("phaseOrder", first.getPhaseOrder());
        value.put("subPhaseCount", data.size());
        value.put("cycleTime", first.getCycleTime());
        value.put("offset", first.getOffset());

        JSONArray subPhases = new JSONArray();
        for (DynamicParameters dp : data) {
            JSONObject subPhaseObj = new JSONObject();
            subPhaseObj.put("subPhaseId", dp.getId().getSubphaseId());
            subPhaseObj.put("green", dp.getGreen());
            subPhaseObj.put("yellow", dp.getYellow());
            subPhaseObj.put("allRed", dp.getAllRed());
            subPhaseObj.put("pedGreenFlash", dp.getPedGreenFlash());
            subPhaseObj.put("pedRed", dp.getPedRed());
            subPhaseObj.put("minGreen", dp.getMinGreen());
            subPhaseObj.put("maxGreen", dp.getMaxGreen());

            subPhases.put(subPhaseObj);
        }

        value.put("subPhases", subPhases);

        JSONObject obj = new JSONObject();
        obj.put("value", value);
        if (!tcSendMessageManager.handle5F15Message(obj)) {
            throw new DynamicException("5F15 dynamic parameters setting failed");
        }

        Thread.sleep(100);
    }

    private void send5F45(String deviceId, int targetPlanId, List<DynamicParameters> data) throws InterruptedException {
        JSONObject value = new JSONObject();
        value.put("deviceId", deviceId);
        value.put("planId", targetPlanId);

        JSONObject obj = new JSONObject();
        obj.put("value", value);

        if (!tcSendMessageManager.handle5F45Message(obj)) {
            throw new DynamicException("5F45 dynamic parameters check failed");
        } else {
            // check 5FC5
            long startTime = System.currentTimeMillis();
            JSONObject value5FC4, value5FC5;
            do {
                value5FC4 = tcReceiveMessageManager.getValueMap5FC4().get(deviceId);
                value5FC5 = tcReceiveMessageManager.getValueMap5FC5().get(deviceId);
            } while ((value5FC4 == null || value5FC5 == null) && (System.currentTimeMillis() - startTime < 16000));

            StringBuilder errorLog = new StringBuilder();

            if (value5FC4 == null || value5FC5 == null) {
                throw new DynamicException("5FC5 null failed");
            } else if (!check5FC5(data, value5FC4, value5FC5, targetPlanId, errorLog)) {
                throw new DynamicException("5FC5 parameter mismatch failed, " + errorLog.toString());
            }

            tcReceiveMessageManager.getValueMap5FC4().remove(deviceId);
            tcReceiveMessageManager.getValueMap5FC5().remove(deviceId);
        }

        Thread.sleep(100);
    }

    private boolean check5FC5(List<DynamicParameters> data, JSONObject value5FC4, JSONObject value5FC5, int targetPlanId, StringBuilder errorLog) {
        try {
            DynamicParameters first = data.getFirst();
            int cycleTime = first.getCycleTime();
            int direct = first.getDirect();
            String phaseOrder = first.getPhaseOrder();
            int subPhaseCount = data.size();
            int offset = first.getOffset();

            boolean basicMatch = true;
            basicMatch &= compareInt("planId (5fc4)", targetPlanId, value5FC4.getInt("planId"), errorLog);
            basicMatch &= compareInt("planId (5fc5)", targetPlanId, value5FC5.getInt("planId"), errorLog);
            basicMatch &= compareInt("cycleTime", cycleTime, value5FC5.getInt("cycleTime"), errorLog);
            basicMatch &= compareInt("direct", direct, value5FC5.getInt("direct"), errorLog);
            basicMatch &= compareString("phaseOrder", phaseOrder, value5FC5.getString("phaseOrder"), errorLog);
            basicMatch &= compareInt("subPhaseCount (5fc4)", subPhaseCount, value5FC4.getInt("subPhaseCount"), errorLog);
            basicMatch &= compareInt("subPhaseCount (5fc5)", subPhaseCount, value5FC5.getInt("subPhaseCount"), errorLog);
            basicMatch &= compareInt("offset", offset, value5FC5.getInt("offset"), errorLog);

            if (!basicMatch) return false;

            // compare lists
            List<Integer> green = new ArrayList<>();
            List<Integer> minGreen = new ArrayList<>();
            List<Integer> maxGreen = new ArrayList<>();
            List<Integer> yellow = new ArrayList<>();
            List<Integer> allRed = new ArrayList<>();
            List<Integer> pedGreenFlash = new ArrayList<>();
            List<Integer> pedRed = new ArrayList<>();

            for (DynamicParameters p : data) {
                green.add(p.getGreen());
                minGreen.add(p.getMinGreen());
                maxGreen.add(p.getMaxGreen());
                yellow.add(p.getYellow());
                allRed.add(p.getAllRed());
                pedGreenFlash.add(p.getPedGreenFlash());
                pedRed.add(p.getPedRed());
            }

            boolean listMatch = true;
            listMatch &= compareList(green, value5FC5.getJSONArray("green"), "green", errorLog);
            listMatch &= compareList(minGreen, value5FC4.getJSONArray("minGreen"), "minGreen", errorLog);
            listMatch &= compareList(maxGreen, value5FC4.getJSONArray("maxGreen"), "maxGreen", errorLog);
            listMatch &= compareList(yellow, value5FC4.getJSONArray("yellow"), "yellow", errorLog);
            listMatch &= compareList(allRed, value5FC4.getJSONArray("allRed"), "allRed", errorLog);
            listMatch &= compareList(pedGreenFlash, value5FC4.getJSONArray("pedGreenFlash"), "pedGreenFlash", errorLog);
            listMatch &= compareList(pedRed, value5FC4.getJSONArray("pedRed"), "pedRed", errorLog);

            return listMatch;

        } catch (Exception e) {
            errorLog.append("Exception: ").append(e.getMessage()).append("\n");
            log.error("Error in check5FC5: {}", e.getMessage());
            return false;
        }
    }

    private void send5F18(String deviceId, int targetPlanId) throws InterruptedException {
        JSONObject value = new JSONObject();
        value.put("deviceId", deviceId);
        value.put("planId", targetPlanId);

        JSONObject obj = new JSONObject();
        obj.put("value", value);
        if (!tcSendMessageManager.handle5F18Message(obj)) {
            throw new DynamicException("5F18 enable target plan ID failed");
        }

        Thread.sleep(100);
    }

    private boolean compareInt(String name, int expected, int actual, StringBuilder errorLog) {
        if (expected != actual) {
            errorLog.append(String.format("%s mismatch: expected=%d, actual=%d%n", name, expected, actual));
            return false;
        }
        return true;
    }

    private boolean compareString(String name, String expected, String actual, StringBuilder errorLog) {
        if (!expected.equalsIgnoreCase(actual)) {
            errorLog.append(String.format("%s mismatch: expected=%s, actual=%s%n", name, expected, actual));
            return false;
        }
        return true;
    }

    private boolean compareList(List<Integer> expected, JSONArray actualArray, String name, StringBuilder errorLog) {
        List<Integer> actual = jsonArrayToList(actualArray);
        if (!expected.equals(actual)) {
            errorLog.append(String.format("%s list mismatch: expected=%s, actual=%s%n", name, expected, actual));
            return false;
        }
        return true;
    }

    private List<Integer> jsonArrayToList(JSONArray array) {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            list.add(array.getInt(i));
        }
        return list;
    }
}
