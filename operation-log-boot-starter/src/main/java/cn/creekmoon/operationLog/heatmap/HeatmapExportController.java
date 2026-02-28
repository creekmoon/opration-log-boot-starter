package cn.creekmoon.operationLog.heatmap;

import cn.creekmoon.operationLog.export.CsvExportService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 热力图数据导出Controller
 * 提供CSV导出接口
 */
@RestController
@RequestMapping("/operation-log/heatmap")
@RequiredArgsConstructor
public class HeatmapExportController {

    private final HeatmapService heatmapService;
    private final CsvExportService csvExportService;

    /**
     * 导出实时统计数据为CSV
     */
    @GetMapping("/export/realtime")
    public void exportRealtimeStats(HttpServletResponse response) throws IOException {
        List<List<String>> data = heatmapService.exportRealtimeStatsToCsv();
        
        String fileName = csvExportService.generateFileName("heatmap-realtime");
        response.setContentType(csvExportService.getContentType());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, 
                "attachment; filename=\"" + fileName + "\"");
        
        try (OutputStream out = response.getOutputStream()) {
            csvExportService.exportCsvWithBom(data.get(0), data.subList(1, data.size()), 
                    row -> row, out);
        }
    }

    /**
     * 导出TopN排行数据为CSV
     */
    @GetMapping("/export/topn")
    public void exportTopN(
            @RequestParam(defaultValue = "REALTIME") HeatmapService.TimeWindow timeWindow,
            @RequestParam(defaultValue = "PV") HeatmapService.MetricType metricType,
            @RequestParam(defaultValue = "10") int topN,
            HttpServletResponse response) throws IOException {
        
        List<List<String>> data = heatmapService.exportTopNToCsv(timeWindow, metricType, topN);
        
        String fileName = csvExportService.generateFileName("heatmap-topn");
        response.setContentType(csvExportService.getContentType());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, 
                "attachment; filename=\"" + fileName + "\"");
        
        try (OutputStream out = response.getOutputStream()) {
            csvExportService.exportCsvWithBom(data.get(0), data.subList(1, data.size()), 
                    row -> row, out);
        }
    }

    /**
     * 导出趋势数据为CSV
     */
    @GetMapping("/export/trend")
    public void exportTrend(
            @RequestParam String className,
            @RequestParam String methodName,
            @RequestParam(defaultValue = "HOURLY") HeatmapService.TimeWindow timeWindow,
            @RequestParam(defaultValue = "24") int pointCount,
            HttpServletResponse response) throws IOException {
        
        List<List<String>> data = heatmapService.exportTrendToCsv(className, methodName, timeWindow, pointCount);
        
        String fileName = csvExportService.generateFileName("heatmap-trend");
        response.setContentType(csvExportService.getContentType());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, 
                "attachment; filename=\"" + fileName + "\"");
        
        try (OutputStream out = response.getOutputStream()) {
            csvExportService.exportCsvWithBom(data.get(0), data.subList(1, data.size()), 
                    row -> row, out);
        }
    }
}
