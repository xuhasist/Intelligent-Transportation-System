package com.demo.manager;

import com.demo.exception.DynamicException;
import com.demo.model.its.TCInfo;
import com.demo.dto.ConditionDto;
import com.demo.dto.ThresholdDto;
import com.demo.dto.TrafficPeriodDto;
import com.demo.repository.its.TCInfoRepository;
import com.demo.service.DynamicService;
import com.demo.service.SocketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DynamicControlManager {
    private static final Logger log = LoggerFactory.getLogger(DynamicControlManager.class);

    @Autowired
    private DynamicService dynamicService;

    @Autowired
    private TCInfoRepository tcInfoRepository;

    @Autowired
    private SocketService socketService;

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
        } catch (Exception e) {
            log.error("Error in checkConditionMatch: {}", e.getMessage());
        }
    }

    public void dynamicTrigger(String program_id, LocalTime startTime, LocalTime endTime, boolean isWeekday) {
        try {
            Map<String, Integer> tcPlanMap = dynamicService.getAllDynamicPlanIds(program_id, isWeekday);

            for (String tc : tcPlanMap.keySet()) {
                int targetPlanId = tcPlanMap.get(tc);

                String host = tcInfoRepository.findByTcId(tc).getIp();
                if (!socketService.isHostConnected(host)) {
                    //saveDynamicLog, message = socket is not connected
                    continue;
                }

                Optional<TCInfo> tcOpt = tcInfoRepository.findById(tc);
                TCInfo tcData = tcOpt.orElse(null);
                if (tcData == null || tcData.getEnable().equals(Byte.valueOf("0"))) {
                    //saveDynamicLog, message = dynamic control not enabled for TC
                    continue;
                }

                int retryCnt = 0;
                int maxRetry = 3; // maximum retry attempts
                boolean success = false;

                while (!success && retryCnt < maxRetry) {
                    try {
                        triggerDynamicControl(program_id, tc, targetPlanId);
                        //saveDynamicLog, message = apply dynamic control success
                        success = true;
                        break;
                    } catch (DynamicException e) {
                        //saveDynamicLog, message = apply dynamic control failed
                    }
                    retryCnt++;
                }

                if (!success) {
                    tryCloseDynamic(tc);    // apply dynamic control failed, close it
                }

                Thread.sleep(500); // avoid too frequent requests
            }
        } catch (Exception e) {
            log.error("Error in dynamicTrigger: {}", e.getMessage());
        }
    }

    private void triggerDynamicControl(String programId, String tcId, int targetPlanId) {
        //saveDynamicLog, message = apply dynamic control for TC

        try {
            /* Apply a sequence of commands to TC devices to trigger dynamic control */
            /*
            send5F10(tcId, 1);                 // enable dynamic control
            send5F40(tcId, 1);                 // check if dynamic control is enabled
            send5F15(tcId, targetPlanId);      // set target plan ID and relevant parameters
            send5F45(tcId, targetPlanId);      // check if target plan ID is set correctly
            send5F18(tcId, targetPlanId);      // enable target plan ID
            */
        } catch (Exception e) {
            throw new DynamicException(e.getMessage());
        }
    }

    private void tryCloseDynamic(String tcId) {
        /*
        send5F10(tcId, 0);   // disable dynamic control
        send5F40(tcId, 0);   // check if dynamic control is disabled
        */
    }
}
