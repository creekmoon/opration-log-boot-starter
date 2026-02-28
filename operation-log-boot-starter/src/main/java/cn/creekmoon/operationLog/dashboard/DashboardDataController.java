package cn.creekmoon.operationLog.dashboard;

import cn.creekmoon.operationLog.heatmap.HeatmapService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Dashboard 数据控制器 V2.5
 * 支持时间筛选和数据联动
 */
@RestController
@RequestMapping("/operation-log/dashboard/api")
@RequiredArgsConstructor
public class DashboardDataController {

    private final HeatmapService heatmapService;
    private final DashboardDataService dashboardDataService;

    /**
     * 获取概览数据
     * 支持时间范围筛选
     */
    @GetMapping("/overview")
    public OverviewData getOverviewData(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : LocalDateTime.now().minusDays(7);
        LocalDateTime end = endDate != null ? endDate.atTime(23, 59, 59) : LocalDateTime.now();
        
        return dashboardDataService.getOverviewData(start, end);
    }

    /**
     * 获取热门操作统计
     * 按 @OperationLog 的 value 聚合
     */
    @GetMapping("/top-operations")
    public List<OperationStat> getTopOperations(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : LocalDateTime.now().minusDays(7);
        LocalDateTime end = endDate != null ? endDate.atTime(23, 59, 59) : LocalDateTime.now();
        
        return dashboardDataService.getTopOperations(start, end, limit);
    }

    /**
     * 获取趋势数据
     */
    @GetMapping("/trend")
    public List<TrendPoint> getTrendData(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : LocalDateTime.now().minusDays(7);
        LocalDateTime end = endDate != null ? endDate.atTime(23, 59, 59) : LocalDateTime.now();
        
        return dashboardDataService.getTrendData(start, end);
    }

    /**
     * 获取操作类型分布
     */
    @GetMapping("/operation-types")
    public Map<String, Long> getOperationTypeDistribution(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : LocalDateTime.now().minusDays(7);
        LocalDateTime end = endDate != null ? endDate.atTime(23, 59, 59) : LocalDateTime.now();
        
        return dashboardDataService.getOperationTypeDistribution(start, end);
    }

    // ==================== DTOs ====================

    public record OverviewData(
            long totalOperations,
            long totalUsers,
            long todayOperations,
            long avgResponseTime,
            double errorRate,
            List<TrendPoint> trend
    ) {}

    public record OperationStat(
            String operationName,
            String operationType,
            long count,
            double percentage,
            long avgResponseTime,
            long errorCount
    ) {}

    public record TrendPoint(
            String date,
            long count,
            long userCount
    ) {}
}
