package cn.creekmoon.operationLog.actuator;

import cn.creekmoon.operationLog.export.CsvExportService;
import cn.creekmoon.operationLog.heatmap.HeatmapService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 热力图Actuator端点
 * 访问路径: /actuator/operation-log-heatmap
 */
@Component
@Endpoint(id = "operation-log-heatmap")
@RequiredArgsConstructor
public class HeatmapActuatorEndpoint {

    private final HeatmapService heatmapService;
    private final CsvExportService csvExportService;

    /**
     * 获取热力图服务状态
     */
    @ReadOperation
    public Map<String, Object> status() {
        Map<String, Object> result = new HashMap<>();
        HeatmapService.HeatmapStatus status = heatmapService.getStatus();
        
        result.put("enabled", status.enabled());
        result.put("redisConnected", status.redisConnected());
        result.put("fallbackActive", status.fallbackActive());
        result.put("totalKeys", status.totalKeys());
        result.put("memoryUsage", status.memoryUsage());
        
        return result;
    }

    /**
     * 获取所有接口的实时统计
     */
    @ReadOperation(operation = "stats")
    public Map<String, HeatmapService.HeatmapStats> allStats() {
        return heatmapService.getAllRealtimeStats();
    }

    /**
     * 获取指定接口的实时统计
     */
    @ReadOperation(operation = "stats/{className}/{methodName}")
    public HeatmapService.HeatmapStats stats(@Selector String className, @Selector String methodName) {
        return heatmapService.getRealtimeStats(className, methodName);
    }

    /**
     * 获取TopN接口
     */
    @ReadOperation(operation = "topn")
    public Map<String, Object> topN() {
        Map<String, Object> result = new HashMap<>();
        
        List<HeatmapService.HeatmapTopItem> topPv = heatmapService.getTopN(
                HeatmapService.TimeWindow.REALTIME, 
                HeatmapService.MetricType.PV, 
                10);
        
        List<HeatmapService.HeatmapTopItem> topUv = heatmapService.getTopN(
                HeatmapService.TimeWindow.REALTIME, 
                HeatmapService.MetricType.UV, 
                10);
        
        result.put("topPv", topPv);
        result.put("topUv", topUv);
        
        return result;
    }

    // ==================== CSV导出端点 ====================

    /**
     * 导出实时统计CSV
     */
    @ReadOperation(operation = "export/realtime")
    public void exportRealtime(HttpServletResponse response) throws IOException {
        List<List<String>> data = heatmapService.exportRealtimeStatsToCsv();
        String fileName = csvExportService.generateFileName("heatmap-realtime");
        
        response.setContentType(csvExportService.getContentType());
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
        
        try (OutputStream out = response.getOutputStream()) {
            csvExportService.exportCsvWithBom(data.get(0), data.subList(1, data.size()), 
                    row -> row, out);
        }
    }

    /**
     * 导出TopN CSV
     */
    @ReadOperation(operation = "export/topn")
    public void exportTopN(HttpServletResponse response) throws IOException {
        List<List<String>> data = heatmapService.exportTopNToCsv(
                HeatmapService.TimeWindow.REALTIME, 
                HeatmapService.MetricType.PV, 
                10);
        String fileName = csvExportService.generateFileName("heatmap-topn");
        
        response.setContentType(csvExportService.getContentType());
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
        
        try (OutputStream out = response.getOutputStream()) {
            csvExportService.exportCsvWithBom(data.get(0), data.subList(1, data.size()), 
                    row -> row, out);
        }
    }
}
