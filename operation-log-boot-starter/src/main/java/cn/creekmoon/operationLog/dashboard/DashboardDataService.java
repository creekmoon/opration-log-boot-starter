package cn.creekmoon.operationLog.dashboard;

import cn.creekmoon.operationLog.heatmap.HeatmapService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Dashboard 数据服务
 */
@Service
@RequiredArgsConstructor
public class DashboardDataService {

    private final HeatmapService heatmapService;

    /**
     * 获取概览数据
     */
    public DashboardDataController.OverviewData getOverviewData(LocalDateTime start, LocalDateTime end) {
        // 获取所有接口统计
        Map<String, HeatmapService.HeatmapStats> allStats = heatmapService.getAllRealtimeStats();
        
        long totalOperations = allStats.values().stream()
            .mapToLong(HeatmapService.HeatmapStats::pv)
            .sum();
        long totalUsers = allStats.values().stream()
            .mapToLong(HeatmapService.HeatmapStats::uv)
            .sum();
        
        return new DashboardDataController.OverviewData(
            totalOperations,
            totalUsers,
            0L,  // todayOperations - 需要实现
            0L,  // avgResponseTime - 需要实现
            0.0, // errorRate - 需要实现
            List.of()  // trend - 需要实现
        );
    }
    
    /**
     * 获取热门操作统计
     */
    public List<DashboardDataController.OperationStat> getTopOperations(LocalDateTime start, LocalDateTime end, int limit) {
        // 从热力图服务获取 TopN 数据
        List<HeatmapService.HeatmapTopItem> topItems = heatmapService.getTopN(
            HeatmapService.TimeWindow.REALTIME,
            HeatmapService.MetricType.PV,
            limit
        );
        
        return topItems.stream()
            .map(item -> new DashboardDataController.OperationStat(
                item.fullName(),
                "DEFAULT",
                item.value(),
                0.0,  // percentage - 需要计算
                0L,   // avgResponseTime - 需要实现
                0L    // errorCount - 需要实现
            ))
            .toList();
    }
    
    /**
     * 获取趋势数据
     */
    public List<DashboardDataController.TrendPoint> getTrendData(LocalDateTime start, LocalDateTime end) {
        // 简化实现，返回空列表
        return List.of();
    }
    
    /**
     * 获取操作类型分布
     */
    public Map<String, Long> getOperationTypeDistribution(LocalDateTime start, LocalDateTime end) {
        // 简化实现，返回空 map
        return Map.of();
    }

    /**
     * 获取实时 Dashboard 数据
     */
    public DashboardRealtimeData getRealtimeData() {
        // 获取所有接口统计
        Map<String, HeatmapService.HeatmapStats> allStats = heatmapService.getAllRealtimeStats();
        
        // 计算总 PV/UV
        long totalPv = allStats.values().stream()
            .mapToLong(HeatmapService.HeatmapStats::pv)
            .sum();
        long totalUv = allStats.values().stream()
            .mapToLong(HeatmapService.HeatmapStats::uv)
            .sum();
        
        // 获取 Top10
        List<HeatmapService.HeatmapTopItem> topPv = heatmapService.getTopN(
            HeatmapService.TimeWindow.REALTIME, 
            HeatmapService.MetricType.PV, 
            10
        );
        
        // 构建趋势数据 (最近 12 个点，每 5 分钟一个)
        List<TrendPoint> trend = generateTrendData();
        
        return new DashboardRealtimeData(
            totalPv,
            totalUv,
            allStats.size(),
            topPv,
            trend,
            LocalDateTime.now()
        );
    }
    
    private List<TrendPoint> generateTrendData() {
        // 从 Redis 或内存中获取历史趋势数据
        // 这里简化实现，实际应从 Redis 时间序列中获取
        return List.of(); // 待实现
    }
    
    /**
     * 实时数据 DTO
     */
    public record DashboardRealtimeData(
        long totalPv,
        long totalUv,
        int activeEndpoints,
        List<HeatmapService.HeatmapTopItem> topPvList,
        List<TrendPoint> trend,
        LocalDateTime timestamp
    ) {}
    
    /**
     * 趋势数据点
     */
    public record TrendPoint(
        String time,
        long pv,
        long uv
    ) {}
}
