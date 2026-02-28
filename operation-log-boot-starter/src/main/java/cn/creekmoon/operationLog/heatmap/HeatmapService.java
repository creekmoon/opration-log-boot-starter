package cn.creekmoon.operationLog.heatmap;

import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 热力图服务接口
 * 提供PV/UV统计、TopN查询、CSV导出等功能
 */
public interface HeatmapService {

    /**
     * 记录一次访问(PV+UV)
     *
     * @param className  类名
     * @param methodName 方法名
     * @param userId     用户ID(用于UV统计)
     * @param timestamp  访问时间
     */
    void recordVisit(String className, String methodName, String userId, LocalDateTime timestamp);

    /**
     * 记录一次访问(使用当前时间)
     *
     * @param className  类名
     * @param methodName 方法名
     * @param userId     用户ID
     */
    void recordVisit(String className, String methodName, String userId);

    /**
     * 获取指定接口的实时PV/UV统计
     *
     * @param className  类名
     * @param methodName 方法名
     * @return PV/UV统计结果
     */
    HeatmapStats getRealtimeStats(String className, String methodName);

    /**
     * 获取所有接口的实时PV/UV统计
     *
     * @return 所有接口的统计结果
     */
    Map<String, HeatmapStats> getAllRealtimeStats();

    /**
     * 获取指定时间段内的TopN接口
     *
     * @param timeWindow 时间窗口类型
     * @param metricType 指标类型(PV/UV)
     * @param topN       返回数量
     * @return TopN接口列表
     */
    List<HeatmapTopItem> getTopN(TimeWindow timeWindow, MetricType metricType, int topN);

    /**
     * 获取指定时间段内的TopN接口(使用默认数量)
     *
     * @param timeWindow 时间窗口类型
     * @param metricType 指标类型
     * @return TopN接口列表
     */
    List<HeatmapTopItem> getTopN(TimeWindow timeWindow, MetricType metricType);

    /**
     * 获取指定接口的历史趋势
     *
     * @param className   类名
     * @param methodName  方法名
     * @param timeWindow  时间窗口类型
     * @param pointCount  数据点数量
     * @return 历史趋势数据
     */
    List<HeatmapTrendPoint> getTrend(String className, String methodName, TimeWindow timeWindow, int pointCount);

    /**
     * 清理过期数据
     */
    void cleanupExpiredData();

    /**
     * 获取服务状态信息
     *
     * @return 状态信息
     */
    HeatmapStatus getStatus();

    // ==================== CSV导出方法 ====================

    /**
     * 导出实时统计数据为CSV格式
     *
     * @return CSV数据行列表（包含表头）
     */
    List<List<String>> exportRealtimeStatsToCsv();

    /**
     * 导出TopN数据为CSV格式
     *
     * @param timeWindow 时间窗口类型
     * @param metricType 指标类型
     * @param topN       返回数量
     * @return CSV数据行列表（包含表头）
     */
    List<List<String>> exportTopNToCsv(TimeWindow timeWindow, MetricType metricType, int topN);

    /**
     * 导出趋势数据为CSV格式
     *
     * @param className   类名
     * @param methodName  方法名
     * @param timeWindow  时间窗口类型
     * @param pointCount  数据点数量
     * @return CSV数据行列表（包含表头）
     */
    List<List<String>> exportTrendToCsv(String className, String methodName, TimeWindow timeWindow, int pointCount);

    /**
     * 指标类型枚举
     */
    enum MetricType {
        PV, UV
    }

    /**
     * 时间窗口类型枚举
     */
    enum TimeWindow {
        REALTIME,   // 实时(最近1小时)
        HOURLY,     // 小时级
        DAILY       // 天级
    }

    /**
     * 热力图统计结果
     */
    record HeatmapStats(
            String className,
            String methodName,
            String fullName,
            long pv,
            long uv,
            LocalDateTime timestamp
    ) {
        public HeatmapStats(String className, String methodName, long pv, long uv, LocalDateTime timestamp) {
            this(className, methodName, className + "." + methodName, pv, uv, timestamp);
        }
    }

    /**
     * TopN接口项
     */
    record HeatmapTopItem(
            int rank,
            String className,
            String methodName,
            String fullName,
            long value,
            MetricType metricType
    ) {
        public HeatmapTopItem(int rank, String className, String methodName, long value, MetricType metricType) {
            this(rank, className, methodName, className + "." + methodName, value, metricType);
        }
    }

    /**
     * 趋势数据点
     */
    record HeatmapTrendPoint(
            LocalDateTime timestamp,
            long pv,
            long uv
    ) {}

    /**
     * 服务状态
     */
    record HeatmapStatus(
            boolean enabled,
            boolean redisConnected,
            boolean fallbackActive,
            long totalKeys,
            long memoryUsage
    ) {}
}
