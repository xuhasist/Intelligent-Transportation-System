package com.demo.service;

import com.demo.dto.OverallPerformanceDto;
import com.demo.dto.PeriodPerformanceDto;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.springframework.jdbc.core.CallableStatementCallback;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.CallableStatementCreator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MonthlyReportServiceTest {
    @InjectMocks
    private MonthlyReportService service;

    @Mock
    private JdbcTemplate jdbcTemplate;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws NoSuchFieldException, IllegalAccessException {
        objectMapper = new ObjectMapper();

        Field field = MonthlyReportService.class.getDeclaredField("objectMapper");
        field.setAccessible(true);
        field.set(service, objectMapper);
    }

    public static class TestDto {
        public String holidayLabel;
        public Integer avgBeforeCount;
    }

    @Test
    void testCallSpRoadTrafficSum() throws Exception {
        CallableStatement cs = mock(CallableStatement.class);
        ResultSet rs = mock(ResultSet.class);
        ResultSetMetaData meta = mock(ResultSetMetaData.class);

        when(jdbcTemplate.execute((CallableStatementCreator) any(), any(CallableStatementCallback.class)))
                .thenAnswer(invocation -> {
                    CallableStatementCallback<List<List<Map<String, Object>>>> callback = invocation.getArgument(1); // get second argument
                    // callback read result from cs, convert to List<List<Map<String, Object>>>
                    return callback.doInCallableStatement(cs);
                });

        // first time return true, second time return false to end the loop
        when(cs.execute()).thenReturn(true, false);
        when(cs.getResultSet()).thenReturn(rs);
        when(rs.getMetaData()).thenReturn(meta);
        when(meta.getColumnCount()).thenReturn(1);
        when(meta.getColumnLabel(1)).thenReturn("col1");
        when(rs.next()).thenReturn(true, false);
        when(rs.getObject(1)).thenReturn("val1");

        List<List<Map<String, Object>>> result = service.callSpRoadTrafficSum("202510", "202511");
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("val1", result.getFirst().getFirst().get("col1"));
    }

    @Test
    void testExtractTableData() {
        List<Map<String, Object>> table = new ArrayList<>();
        Map<String, Object> row = new HashMap<>();
        row.put("平假日", "平日");
        row.put("事前資料平均筆數", 10);
        table.add(row);

        List<List<Map<String, Object>>> results = new ArrayList<>();
        results.add(table);

        List<TestDto> dtos = service.extractTableData(results, TestDto.class, 0);
        assertEquals(1, dtos.size());
        assertEquals("平日", dtos.getFirst().holidayLabel);
        assertEquals(10, dtos.getFirst().avgBeforeCount);
    }

    @Test
    void testExportExcel() throws Exception {
        List<OverallPerformanceDto> table1 = List.of(new OverallPerformanceDto() {{
            setTotalPerformanceRate(50.5);
        }});
        List<PeriodPerformanceDto> table2 = List.of(new PeriodPerformanceDto() {{
            setHolidayLabel("平日");
            setPeriodLabel("早上");
            setAvgBeforeCount(10.0);
            setAvgAfterCount(20.0);
            setSegmentPerformanceRate(50.0);
        }});

        MockHttpServletResponse response = new MockHttpServletResponse();

        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class);
             MockedConstruction<XSSFWorkbook> workbookMocked =
                     mockConstruction(XSSFWorkbook.class, (mockWorkbook, context) -> {
                         // mock XSSFSheet
                         XSSFSheet mockSheet = mock(XSSFSheet.class);
                         XSSFRow mockRow = mock(XSSFRow.class);
                         XSSFCell mockCell = mock(XSSFCell.class);

                         when(mockWorkbook.createSheet(anyString())).thenReturn(mockSheet);
                         when(mockSheet.createRow(anyInt())).thenReturn(mockRow);
                         when(mockRow.createCell(anyInt())).thenReturn(mockCell);
                     })) {

            mockedFiles.when(() -> Files.createDirectories(any(Path.class)))
                    .thenReturn(Path.of("/mock/path"));
            mockedFiles.when(() -> Files.exists(any(Path.class)))
                    .thenReturn(true);

            service.exportExcel(table1, table2, "202510", response);

            // verify workbook is constructed once
            assertEquals(1, workbookMocked.constructed().size());

            // verify response header
            assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    response.getContentType());
            assertTrue(response.getHeader("Content-Disposition").contains("202510_performance.xlsx"));
        }
    }
}
