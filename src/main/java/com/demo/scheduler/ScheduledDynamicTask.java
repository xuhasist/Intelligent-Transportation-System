package com.demo.scheduler;

import com.demo.manager.DynamicControlManager;
import com.demo.model.dynamic.DynamicCondition;
import com.demo.model.dynamic.DynamicThreshold;
import com.demo.dto.TrafficPeriodDto;
import com.demo.service.DynamicService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Component
public class ScheduledDynamicTask {
    public final static long ONE_Minute = 60 * 1000;
    public final static long FIVE_Minute = 5 * 60 * 1000;

    @Autowired
    private DynamicService dynamicService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DynamicControlManager dynamicControlManager;

    // every 10 minutes check if time periods matches or not
    // if matches, then apply dynamic controls
    @Async
    @Scheduled(cron = "0 0/10 * * * ?")
    //@Scheduled(fixedRate = 30 * 60 * 1000, initialDelay = 3000)
    public void checkTrafficPeriods() {
        LocalDateTime now = LocalDateTime.now();
        DayOfWeek dayOfWeek = now.getDayOfWeek();
        boolean isWeekday = (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY);

        List<TrafficPeriodDto> activePeriods = dynamicService.getTrafficPeriodsMap().get(isWeekday);
        for (TrafficPeriodDto period : activePeriods) {
            if (period.isInSchedule()) continue;     // running dynamic control, skip it

            LocalTime start = period.getStartTime();
            LocalTime end = period.getEndTime();

            if (dynamicService.isInTrafficPeriod(start, end)) {
                //log.info("Current time is in range: " + start + " - " + end);
                dynamicControlManager.startTrafficCalculation(period, isWeekday);
            }
        }
    }

    @Async
    @PostConstruct  // Initialize the task when the application starts
    @Scheduled(cron = "0 0 0 * * ?")    // Update daily at midnight
    public void readDynamicInfo() {
        List<DynamicCondition> allDynamicCondition = dynamicService.getAllDynamicConditions();
        List<DynamicThreshold> allDynamicThreshold = dynamicService.getAllDynamicThresholds();
        dynamicService.createConditionMap(allDynamicCondition);
        dynamicService.createTrafficAndThresholdMap(allDynamicThreshold);
    }
}
