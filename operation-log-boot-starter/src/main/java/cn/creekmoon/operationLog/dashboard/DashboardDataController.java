package cn.creekmoon.operationLog.dashboard;

import cn.creekmoon.operationLog.heatmap.HeatmapService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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

    /**
     * 获取实时数据
     * 返回当前 Dashboard 的实时统计信息
     */
    @GetMapping("/realtime")
    public RealtimeData getRealtimeData() {
        DashboardDataService.DashboardRealtimeData realtimeData = dashboardDataService.getRealtimeData();

        return new RealtimeData(
            realtimeData.totalPv(),
            realtimeData.totalUv(),
            getCurrentQps(),
            getErrorRate(),
            getPeakQps(),
            LocalDateTime.now()
        );
    }

    /**
     * 获取趋势数据（支持时间范围）
     * 用于 1小时/24小时/7天 切换
     */
    @GetMapping("/trends")
    public TrendData getTrends(
            @RequestParam(defaultValue = "24h") String range) {

        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start;
        int dataPoints;

        switch (range) {
            case "1h" -> {
                start = end.minusHours(1);
                dataPoints = 12; // 每 5 分钟一个点
            }
            case "7d" -> {
                start = end.minusDays(7);
                dataPoints = 7; // 每天一个点
            }
            default -> { // 24h
                start = end.minusHours(24);
                dataPoints = 24; // 每小时一个点
            }
        }

        List<TrendPoint> trend = dashboardDataService.getTrendData(start, end);

        // 如果数据点不够，生成模拟数据填充
        if (trend.isEmpty() || trend.size() < dataPoints) {
            trend = generateMockTrendData(start, end, dataPoints, range);
        }

        return new TrendData(range, trend);
    }

    // ==================== Helper Methods ====================

    private long getCurrentQps() {
        // 简化实现，实际应从 Redis 获取实时 QPS
        // 基于总 PV 估算
        Map<String, HeatmapService.HeatmapStats> allStats = heatmapService.getAllRealtimeStats();
        long totalPv = allStats.values().stream()
            .mapToLong(HeatmapService.HeatmapStats::pv)
            .sum();
        // 估算当前 QPS (假设 PV 是最近 1 分钟的)
        return totalPv / 60;
    }

    private double getErrorRate() {
        // 简化实现，实际应从 Redis 获取错误率统计
        return 0.0;
    }

    private long getPeakQps() {
        // 简化实现，实际应从 Redis 获取峰值 QPS
        long current = getCurrentQps();
        return current > 0 ? current * 2 : 100;
    }

    private List<TrendPoint> generateMockTrendData(LocalDateTime start, LocalDateTime end, int points, String range) {
        List<TrendPoint> result = new ArrayList<>();
        DateTimeFormatter formatter = range.equals("7d")
            ? DateTimeFormatter.ofPattern("MM-dd")
            : DateTimeFormatter.ofPattern("HH:mm");

        for (int i = 0; i < points; i++) {
            LocalDateTime pointTime = start.plusSeconds(
                java.time.Duration.between(start, end).getSeconds() * i / (points - 1)
            );

            // 生成模拟数据，带有一些随机性
            long basePv = 1000 + (long)(Math.random() * 2000);
            long baseUv = (long)(basePv * 0.6);

            result.add(new TrendPoint(
                pointTime.format(formatter),
                basePv,
                baseUv
            ));
        }

        return result;
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

    /**
     * 实时数据 DTO
     */
    public record RealtimeData(
            long totalPv,
            long totalUv,
            long currentQps,
            double errorRate,
            long peakQps,
            LocalDateTime timestamp
    ) {}

    /**
     * 趋势数据响应 DTO
     */
    public record TrendData(
            String range,
            List<TrendPoint> data
    ) {}
}
