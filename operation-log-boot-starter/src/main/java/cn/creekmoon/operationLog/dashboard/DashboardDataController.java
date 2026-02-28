package cn.creekmoon.operationLog.dashboard;

import cn.creekmoon.operationLog.export.CsvExportService;
import cn.creekmoon.operationLog.heatmap.HeatmapExtensionService;
import cn.creekmoon.operationLog.heatmap.HeatmapService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 扩展Dashboard数据API
 * 提供丰富的数据维度和导出功能
 */
@RestController
@RequestMapping("/operation-log/dashboard/api")
@RequiredArgsConstructor
public class DashboardDataController {

    private final HeatmapExtensionService heatmapExtensionService;
    private final HeatmapService heatmapService;
    private final CsvExportService csvExportService;

    /**
     * 获取响应时间统计
     */
    @GetMapping("/response-time")
    public HeatmapExtensionService.ResponseTimeStats getResponseTime(
            @RequestParam String className,
            @RequestParam String methodName,
            @RequestParam(defaultValue = "REALTIME") HeatmapService.TimeWindow timeWindow) {
        return heatmapExtensionService.getResponseTimeStats(className, methodName, timeWindow);
    }

    /**
     * 获取错误率趋势
     */
    @GetMapping("/error-rate")
    public Map<LocalDateTime, Double> getErrorRate(
            @RequestParam String className,
            @RequestParam String methodName,
            @RequestParam(defaultValue = "REALTIME") HeatmapService.TimeWindow timeWindow,
            @RequestParam(defaultValue = "24") int points) {
        return heatmapExtensionService.getErrorRateTrend(className, methodName, timeWindow, points);
    }

    /**
     * 获取地域分布
     */
    @GetMapping("/geo-distribution")
    public Map<String, Long> getGeoDistribution(
            @RequestParam(defaultValue = "REALTIME") HeatmapService.TimeWindow timeWindow) {
        return heatmapExtensionService.getGeoDistribution(timeWindow);
    }

    /**
     * 获取终端分布
     */
    @GetMapping("/terminal-distribution")
    public Map<String, Long> getTerminalDistribution(
            @RequestParam(defaultValue = "REALTIME") HeatmapService.TimeWindow timeWindow) {
        return heatmapExtensionService.getTerminalDistribution(timeWindow);
    }

    /**
     * 导出响应时间CSV
     */
    @GetMapping("/export/response-time")
    public void exportResponseTime(
            @RequestParam String className,
            @RequestParam String methodName,
            @RequestParam(defaultValue = "REALTIME") HeatmapService.TimeWindow timeWindow,
            HttpServletResponse response) throws IOException {
        
        HeatmapExtensionService.ResponseTimeStats stats = 
                heatmapExtensionService.getResponseTimeStats(className, methodName, timeWindow);
        
        List<List<String>> data = new ArrayList<>();
        data.add(List.of("接口类", "接口方法", "P50(ms)", "P95(ms)", "P99(ms)", "平均值", "最大值", "最小值"));
        data.add(List.of(
                stats.className(), stats.methodName(),
                String.valueOf(stats.p50()), String.valueOf(stats.p95()),
                String.valueOf(stats.p99()), String.valueOf(stats.avg()),
                String.valueOf(stats.max()), String.valueOf(stats.min())
        ));
        
        exportCsv(response, data, "response-time");
    }

    /**
     * 导出错误率趋势CSV
     */
    @GetMapping("/export/error-rate")
    public void exportErrorRate(
            @RequestParam String className,
            @RequestParam String methodName,
            @RequestParam(defaultValue = "REALTIME") HeatmapService.TimeWindow timeWindow,
            @RequestParam(defaultValue = "24") int points,
            HttpServletResponse response) throws IOException {
        
        Map<LocalDateTime, Double> trend = 
                heatmapExtensionService.getErrorRateTrend(className, methodName, timeWindow, points);
        
        List<List<String>> data = new ArrayList<>();
        data.add(List.of("时间", "错误率"));
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        trend.forEach((time, rate) -> 
                data.add(List.of(time.format(formatter), String.format("%.4f", rate))));
        
        exportCsv(response, data, "error-rate");
    }

    /**
     * 导出地域分布CSV
     */
    @GetMapping("/export/geo-distribution")
    public void exportGeoDistribution(
            @RequestParam(defaultValue = "REALTIME") HeatmapService.TimeWindow timeWindow,
            HttpServletResponse response) throws IOException {
        
        Map<String, Long> distribution = heatmapExtensionService.getGeoDistribution(timeWindow);
        
        List<List<String>> data = new ArrayList<>();
        data.add(List.of("地域", "访问量"));
        distribution.forEach((region, count) -> data.add(List.of(region, String.valueOf(count))));
        
        exportCsv(response, data, "geo-distribution");
    }

    /**
     * 导出终端分布CSV
     */
    @GetMapping("/export/terminal-distribution")
    public void exportTerminalDistribution(
            @RequestParam(defaultValue = "REALTIME") HeatmapService.TimeWindow timeWindow,
            HttpServletResponse response) throws IOException {
        
        Map<String, Long> distribution = heatmapExtensionService.getTerminalDistribution(timeWindow);
        
        List<List<String>> data = new ArrayList<>();
        data.add(List.of("终端类型", "访问量"));
        distribution.forEach((terminal, count) -> data.add(List.of(terminal, String.valueOf(count))));
        
        exportCsv(response, data, "terminal-distribution");
    }

    private void exportCsv(HttpServletResponse response, List<List<String>> data, String prefix) throws IOException {
        String fileName = csvExportService.generateFileName(prefix);
        response.setContentType(csvExportService.getContentType());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, 
                "attachment; filename=\"" + fileName + "\"");
        
        try (OutputStream out = response.getOutputStream()) {
            csvExportService.exportCsvWithBom(data.get(0), data.subList(1, data.size()), 
                    row -> row, out);
        }
    }
}
