package cn.creekmoon.operationLog.heatmap;

import cn.creekmoon.operationLog.export.CsvExportService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 热力图导出Controller测试类
 */
@ExtendWith(MockitoExtension.class)
class HeatmapExportControllerTest {

    @Mock
    private HeatmapService heatmapService;

    @Mock
    private CsvExportService csvExportService;

    @Mock
    private HttpServletResponse response;

    private HeatmapExportController controller;

    @BeforeEach
    void setUp() {
        controller = new HeatmapExportController(heatmapService, csvExportService);
    }

    @Test
    void testExportRealtimeStats() throws IOException {
        // Given
        List<List<String>> mockData = Arrays.asList(
                Arrays.asList("接口类", "接口方法", "PV", "UV"),
                Arrays.asList("OrderController", "list", "100", "50")
        );
        when(heatmapService.exportRealtimeStatsToCsv()).thenReturn(mockData);
        when(csvExportService.generateFileName(anyString())).thenReturn("heatmap-realtime-20240228-120000.csv");
        when(csvExportService.getContentType()).thenReturn("text/csv;charset=UTF-8");

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        when(response.getOutputStream()).thenReturn(new ServletOutputStreamWrapper(outputStream));

        // When
        controller.exportRealtimeStats(response);

        // Then
        verify(heatmapService).exportRealtimeStatsToCsv();
        verify(csvExportService).generateFileName("heatmap-realtime");
        verify(response).setContentType("text/csv;charset=UTF-8");
    }

    @Test
    void testExportTopN() throws IOException {
        // Given
        List<List<String>> mockData = Arrays.asList(
                Arrays.asList("排名", "接口类", "接口方法", "指标类型", "数值"),
                Arrays.asList("1", "OrderController", "list", "PV", "1000")
        );
        when(heatmapService.exportTopNToCsv(any(), any(), anyInt())).thenReturn(mockData);
        when(csvExportService.generateFileName(anyString())).thenReturn("heatmap-topn-20240228-120000.csv");

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        when(response.getOutputStream()).thenReturn(new ServletOutputStreamWrapper(outputStream));

        // When
        controller.exportTopN(HeatmapService.TimeWindow.REALTIME, HeatmapService.MetricType.PV, 10, response);

        // Then
        verify(heatmapService).exportTopNToCsv(HeatmapService.TimeWindow.REALTIME, HeatmapService.MetricType.PV, 10);
    }

    @Test
    void testExportTrend() throws IOException {
        // Given
        List<List<String>> mockData = Arrays.asList(
                Arrays.asList("时间", "PV", "UV"),
                Arrays.asList("2024-02-28 10:00:00", "100", "50")
        );
        when(heatmapService.exportTrendToCsv(anyString(), anyString(), any(), anyInt())).thenReturn(mockData);
        when(csvExportService.generateFileName(anyString())).thenReturn("heatmap-trend-20240228-120000.csv");

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        when(response.getOutputStream()).thenReturn(new ServletOutputStreamWrapper(outputStream));

        // When
        controller.exportTrend("OrderController", "list", HeatmapService.TimeWindow.HOURLY, 24, response);

        // Then
        verify(heatmapService).exportTrendToCsv("OrderController", "list", HeatmapService.TimeWindow.HOURLY, 24);
    }

    /**
     * 简单的ServletOutputStream包装类用于测试
     */
    static class ServletOutputStreamWrapper extends jakarta.servlet.ServletOutputStream {
        private final ByteArrayOutputStream outputStream;

        public ServletOutputStreamWrapper(ByteArrayOutputStream outputStream) {
            this.outputStream = outputStream;
        }

        @Override
        public void write(int b) throws IOException {
            outputStream.write(b);
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setWriteListener(jakarta.servlet.WriteListener writeListener) {
        }
    }
}
