package com.demo.service;

import com.demo.controller.MonthlyReportController;
import com.demo.dto.OverallPerformanceDto;
import com.demo.dto.PeriodPerformanceDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.CallableStatementCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MonthlyReportService {
    @Autowired
    @Qualifier("trafficJdbcTemplate")
    private JdbcTemplate trafficJdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    public List<List<Map<String, Object>>> callSpRoadTrafficSum(String startTime, String endTime) {
        // Stored Procedure, multiple result sets
        return trafficJdbcTemplate.execute(
                con -> {
                    CallableStatement cs = con.prepareCall("{CALL sp_road_traffic_sum(?, ?)}");
                    cs.setString(1, startTime);
                    cs.setString(2, endTime);
                    return cs;
                },
                (CallableStatementCallback<List<List<Map<String, Object>>>>) cs -> {
                    List<List<Map<String, Object>>> resultSets = new ArrayList<>();

                    boolean hasResult = cs.execute();

                    while (hasResult) {
                        try (ResultSet rs = cs.getResultSet()) {    // table
                            ResultSetMetaData meta = rs.getMetaData();
                            List<Map<String, Object>> rows = new ArrayList<>();
                            while (rs.next()) { // row
                                Map<String, Object> row = new HashMap<>();
                                for (int i = 1; i <= meta.getColumnCount(); i++) {  // column
                                    row.put(meta.getColumnLabel(i), rs.getObject(i));
                                }
                                rows.add(row);
                            }
                            resultSets.add(rows);
                        }
                        hasResult = cs.getMoreResults();
                    }
                    return resultSets;
                }
        );
    }

    public void exportExcel(List<OverallPerformanceDto> table1, List<PeriodPerformanceDto> table2, String yearMonthId, HttpServletResponse response) throws Exception {
        String[] headers = {"平假日", "時段", "事前資料平均筆數", "事後資料平均筆數", "路段改善率(%)"};
        String finalFilePath = "/data/" + yearMonthId + "_performance.xlsx";

        File file = new File(finalFilePath);
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            boolean created = parentDir.mkdirs();
            if (!created) {
                throw new Exception("Create folder failed：" + parentDir.getAbsolutePath());
            }
        }

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Performance Report");

        // set headers
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
        }

        // write data from table2
        int rowIndex = 1;
        for (PeriodPerformanceDto dto : table2) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(dto.getHolidayLabel());
            row.createCell(1).setCellValue(dto.getPeriodLabel());
            row.createCell(2).setCellValue(dto.getAvgBeforeCount());
            row.createCell(3).setCellValue(dto.getAvgAfterCount());
            row.createCell(4).setCellValue(dto.getSegmentPerformanceRate());
        }

        // merge cells for the last row
        Row summaryRow = sheet.createRow(rowIndex);
        summaryRow.createCell(0).setCellValue("績效總改善率");
        summaryRow.createCell(4).setCellValue(table1.getFirst().getTotalPerformanceRate());
        // merge cells from column 0 to 3 in the last row
        sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 3));

        // auto size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        // set response headers for file download
        String filename = yearMonthId + "_performance.xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        // 輸出到 response stream
        try (ServletOutputStream out = response.getOutputStream()) {
            workbook.write(out);
            out.flush();
        }

        workbook.close();
    }

    public <T> List<T> extractTableData(List<List<Map<String, Object>>> results, Class<T> classType, int tableIndex) {
        return results.get(tableIndex).stream()
                .map(row -> {
                    Map<String, Object> convertedRow = new HashMap<>();
                    row.forEach((k, v) -> {
                        String mappedKey = MonthlyReportController.CHINESE_TO_ENGLISH_COLUMNS.get(k);
                        if (mappedKey != null) {
                            convertedRow.put(mappedKey, v);
                        }
                    });
                    return objectMapper.convertValue(convertedRow, classType);
                })
                .collect(Collectors.toList());
    }
}
