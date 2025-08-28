package com.demo.controller;

import com.demo.enums.AuthDefine;
import com.demo.exception.CustomException;
import com.demo.dto.OverallPerformanceDto;
import com.demo.dto.PeriodPerformanceDto;
import com.demo.service.JwtTokenService;
import com.demo.service.MonthlyReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/monthlyReport")
public class MonthlyReportController {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private MonthlyReportService monthlyReportService;

    public static final Map<String, String> CHINESE_TO_ENGLISH_COLUMNS;

    static {
        // can't be modified
        CHINESE_TO_ENGLISH_COLUMNS = Map.of(
                "平假日", "holidayLabel",
                "時段", "periodLabel",
                "事前資料平均筆數", "avgBeforeCount",
                "事後資料平均筆數", "avgAfterCount",
                "路段改善率(%)", "segmentPerformanceRate",
                "績效總改善率", "totalPerformanceRate");
    }

    @Operation(summary = "分時路段總績效", security = @SecurityRequirement(name = "Bearer Authentication"))
    @GetMapping("/getTotalPerformance")
    public void getTotalPerformance(
            HttpServletResponse response,
            HttpServletRequest request,
            @RequestParam(name = "yearMonth")
            @Parameter(description = "Year and month in format yyyy-MM", example = "2025-06")
            String yearMonth
    ) throws Exception {

        if (jwtTokenService.needsAuthentication(request)) {
            throw new CustomException(AuthDefine.InvalidToken.getDescription(), HttpStatus.UNAUTHORIZED);
        }

        YearMonth ym = YearMonth.parse(yearMonth);

        // first day of the month
        LocalDateTime startDateTime = ym.atDay(1).atStartOfDay();
        // last day of the month
        LocalDateTime endDateTime = ym.atEndOfMonth().atTime(23, 59, 59);

        String start = startDateTime.format(formatter);
        String end = endDateTime.format(formatter);

        /*
         * List<List<Map<String, Object>>> represent：
         * outside List：each table
         * middle List：each row in the table
         * inner Map<String, Object>：column name and value
         */
        List<List<Map<String, Object>>> data = monthlyReportService.callSpRoadTrafficSum(start, end);
        List<OverallPerformanceDto> table1 = monthlyReportService.extractTableData(data, OverallPerformanceDto.class, 0);
        List<PeriodPerformanceDto> table2 = monthlyReportService.extractTableData(data, PeriodPerformanceDto.class, 1);

        String yearMonthId = yearMonth.replace("-", "");
        monthlyReportService.exportExcel(table1, table2, yearMonthId, response);
    }
}

