package com.demo.service;

import com.demo.enums.ErrorDefine;
import com.demo.exception.CustomException;
import com.demo.model.dynamic.*;
import com.demo.dto.ConditionDto;
import com.demo.dto.ThresholdDto;
import com.demo.dto.TrafficPeriodDto;
import com.demo.repository.dynamic.*;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class DynamicService {
    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HHmm");

    // String: programId
    @Getter
    private final Map<String, ConditionDto> conditionMap = new ConcurrentHashMap<>();
    // Boolean: weekday:1 or weekend:0
    @Getter
    private final Map<Boolean, List<TrafficPeriodDto>> trafficPeriodsMap = new ConcurrentHashMap<>();
    // Integer: programId-subId
    @Getter
    private final Map<String, ThresholdDto> thresholdMap = new ConcurrentHashMap<>();

    @Autowired
    private DynamicConditionRepository dynamicConditionRepository;

    @Autowired
    private DynamicThresholdRepository dynamicThresholdRepository;

    @Autowired
    private CctvCarflowByPositionRepository CctvCarflowByPositionRepository;

    @Autowired
    private DynamicPlanidRepository dynamicPlanidRepository;

    @Autowired
    private DynamicLogRepository dynamicLogRepository;

    @Autowired
    private DynamicParametersRepository dynamicParametersRepository;


    public List<DynamicCondition> getAllDynamicConditions() {
        return dynamicConditionRepository.findAll();
    }

    public List<DynamicThreshold> getAllDynamicThresholds() {
        return dynamicThresholdRepository.findAll();
    }

    public Map<String, Integer> getAllDynamicPlanIds(String program_id, boolean isWeekday) {
        List<DynamicPlanid> entries = dynamicPlanidRepository.findByProgramIdAndDay(program_id, isWeekday ? "平日" : "假日");
        Map<String, Integer> tcPlanIdMap = new HashMap<>();

        for (DynamicPlanid entry : entries) {
            String[] times = entry.getTime().split("-");  // e.g. "0000-0630"

            LocalTime startTime = LocalTime.parse(times[0].strip(), timeFormatter);
            LocalTime endTime = LocalTime.parse(times[1].strip(), timeFormatter);

            if (isInTrafficPeriod(startTime, endTime)) {
                tcPlanIdMap.put(entry.getTcId(), entry.getPlanId());
            }
        }

        return tcPlanIdMap;
    }

    public DynamicThreshold updateDynamicThreshold(DynamicThreshold dynamicThreshold) {
        if (dynamicThreshold == null || dynamicThreshold.getId() == null) {
            return null;
        }

        Optional<DynamicThreshold> optional = dynamicThresholdRepository.findById(dynamicThreshold.getId());
        if (optional.isPresent()) {
            DynamicThreshold existingThreshold = optional.get();
            existingThreshold.setTimeLabel(dynamicThreshold.getTimeLabel());
            existingThreshold.setCctvId(dynamicThreshold.getCctvId());
            existingThreshold.setCarflowDirection(dynamicThreshold.getCarflowDirection());
            existingThreshold.setTimeInterval(dynamicThreshold.getTimeInterval());
            existingThreshold.setComparisonOperator(dynamicThreshold.getComparisonOperator());
            existingThreshold.setThresholdValue(dynamicThreshold.getThresholdValue());
            return dynamicThresholdRepository.save(existingThreshold);
        } else {
            throw new CustomException(ErrorDefine.DataNotFound.getDescription(), HttpStatus.NOT_FOUND);
        }
    }

    public List<DynamicParameters> getEntriesByProgramIdAndDeviceIdAndPlanId(String programId, String deviceId, int planId) {
        return dynamicParametersRepository.findByIdProgramIdAndIdDeviceIdAndIdPlanId(programId, deviceId, planId);
    }

    public void saveDynamicLog(String programId, String tcId, int planId, String status, String message) {
        DynamicLog log = DynamicLog.builder()
                .programId(programId)
                .tcId(tcId)
                .planId(planId)
                .status(status)
                .message(message)
                .build();
        dynamicLogRepository.save(log);
    }

    public void createConditionMap(List<DynamicCondition> conditions) {
        if (conditions == null) return;

        conditionMap.clear();
        for (DynamicCondition entry : conditions) {
            if (entry == null) continue;

            ConditionDto conditionDto = ConditionDto.builder()
                    .conditionExpression(entry.getConditionExpression())
                    .consecutiveMatches(entry.getConsecutiveMatches())
                    .consecutiveCounts(new AtomicInteger(0))
                    .lastTriggeredTime(new AtomicLong(0))
                    .build();

            conditionMap.putIfAbsent(entry.getProgramId(), conditionDto);
        }
    }

    public void createTrafficAndThresholdMap(List<DynamicThreshold> dynamicThreshold) {
        trafficPeriodsMap.clear();
        thresholdMap.clear();
        for (DynamicThreshold entry : dynamicThreshold) {
            // 平日0600-0630,平日0900-1230,平日1300-1400,平日1900-0600,假日1800-1030,假日1200-1600
            String[] timePeriods = entry.getTimeLabel().split(",");

            for (String timePeriod : timePeriods) {
                Boolean isWeekday = timePeriod.startsWith("平日");

                String[] timeRange = timePeriod.substring(2).split("-");
                LocalTime startTime = LocalTime.parse(timeRange[0], DateTimeFormatter.ofPattern("HHmm"));
                LocalTime endTime = LocalTime.parse(timeRange[1], DateTimeFormatter.ofPattern("HHmm"));

                TrafficPeriodDto trafficPeriodDto = TrafficPeriodDto.builder()
                        .programId(entry.getId().getProgramId())
                        .subId(entry.getId().getSubId())
                        .startTime(startTime)
                        .endTime(endTime)
                        .inSchedule(new AtomicBoolean(false))
                        .build();

                trafficPeriodsMap.computeIfAbsent(isWeekday, k -> new java.util.ArrayList<>()).add(trafficPeriodDto);
            }

            String[] cctvIds = entry.getCctvId().split(",");
            List<String> cctvList = Arrays.stream(cctvIds).map(String::strip).toList();

            String[] carflowDirection = entry.getCarflowDirection().split(",");
            List<String> carflowDirectionList = Arrays.stream(carflowDirection).map(String::strip).toList();

            ThresholdDto thresholdDto = ThresholdDto.builder()
                    .cctvList(cctvList)
                    .timeInterval(entry.getTimeInterval())
                    .carflowDirectionList(carflowDirectionList)
                    .comparisonOperator(entry.getComparisonOperator())
                    .thresholdValue(entry.getThresholdValue())
                    .isMatch(false)
                    .build();

            String id = entry.getId().getProgramId() + "-" + entry.getId().getSubId();
            thresholdMap.putIfAbsent(id, thresholdDto);
        }
    }

    public double getTotalCarFlow(String cctvId, LocalDateTime endTime, int timeInterval_minute) {
        return CctvCarflowByPositionRepository.findCarflowSumByCctvIdAndEndTime(cctvId, endTime, timeInterval_minute);
    }

    public double getSegmentCarFlow(String cctvId, LocalDateTime endTime, int timeInterval_minute, String startPosition, String endPosition) {
        return CctvCarflowByPositionRepository.findCarflowSumByCctvIdAndEndTimeAndStartPositionAndEndPosition(cctvId, endTime, timeInterval_minute, startPosition, endPosition);
    }

    public Boolean isInTrafficPeriod(LocalTime start, LocalTime end) {
        LocalTime now = LocalTime.now();

        if (start.isBefore(end)) {   // normal case
            return !now.isBefore(start) && !now.isAfter(end);
        } else {  // cross midnight case
            return !now.isBefore(start) || !now.isAfter(end);
        }
    }

}
