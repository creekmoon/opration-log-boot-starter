package cn.creekmoon.operationLog.export;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CSV导出服务测试类
 */
class CsvExportServiceImplTest {

    private CsvExportServiceImpl csvExportService;

    @BeforeEach
    void setUp() {
        csvExportService = new CsvExportServiceImpl();
    }

    @Test
    void testExportCsv() throws IOException {
        // Given
        List<String> headers = Arrays.asList("姓名", "年龄", "城市");
        List<List<String>> data = Arrays.asList(
                Arrays.asList("张三", "25", "北京"),
                Arrays.asList("李四", "30", "上海"),
                Arrays.asList("王五", "28", "广州")
        );

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // When
        csvExportService.exportCsv(headers, data, row -> row, outputStream);

        // Then
        String result = outputStream.toString("UTF-8");
        assertTrue(result.contains("姓名,年龄,城市"));
        assertTrue(result.contains("张三,25,北京"));
        assertTrue(result.contains("李四,30,上海"));
        assertTrue(result.contains("王五,28,广州"));
    }

    @Test
    void testExportCsvWithBom() throws IOException {
        // Given
        List<String> headers = Arrays.asList("姓名", "年龄");
        List<List<String>> data = Arrays.asList(
                Arrays.asList("张三", "25")
        );

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // When
        csvExportService.exportCsvWithBom(headers, data, row -> row, outputStream);

        // Then
        byte[] result = outputStream.toByteArray();
        // 检查BOM (EF BB BF)
        assertEquals(0xEF, result[0] & 0xFF);
        assertEquals(0xBB, result[1] & 0xFF);
        assertEquals(0xBF, result[2] & 0xFF);
    }

    @Test
    void testGenerateFileName() {
        // When
        String fileName = csvExportService.generateFileName("test");

        // Then
        assertTrue(fileName.startsWith("test-"));
        assertTrue(fileName.endsWith(".csv"));
        assertTrue(fileName.length() > "test-.csv".length());
    }

    @Test
    void testGetContentType() {
        // When
        String contentType = csvExportService.getContentType();

        // Then
        assertEquals("text/csv;charset=UTF-8", contentType);
    }

    @Test
    void testExportCsvWithEmptyData() throws IOException {
        // Given
        List<String> headers = Arrays.asList("列1", "列2");
        List<List<String>> data = Arrays.asList();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // When
        csvExportService.exportCsv(headers, data, row -> row, outputStream);

        // Then
        String result = outputStream.toString("UTF-8");
        assertTrue(result.contains("列1,列2"));
    }

    @Test
    void testExportCsvWithSpecialCharacters(@TempDir Path tempDir) throws IOException {
        // Given
        List<String> headers = Arrays.asList("描述", "备注");
        List<List<String>> data = Arrays.asList(
                Arrays.asList("包含,逗号", "包含\"引号"),
                Arrays.asList("包含\n换行", "正常文本")
        );

        Path outputFile = tempDir.resolve("special.csv");
        try (FileOutputStream fos = new FileOutputStream(outputFile.toFile())) {
            // When
            csvExportService.exportCsv(headers, data, row -> row, fos);
        }

        // Then - 文件应该成功创建且不为空
        assertTrue(outputFile.toFile().exists());
        assertTrue(outputFile.toFile().length() > 0);
    }
}
