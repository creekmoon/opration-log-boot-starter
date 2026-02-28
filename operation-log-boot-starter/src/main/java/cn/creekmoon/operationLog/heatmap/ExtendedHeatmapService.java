package cn.creekmoon.operationLog.heatmap;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 扩展的热力图服务接口
 * 新增响应时间、错误率等维度
 */
public interface ExtendedHeatmapService extends HeatmapService {

    /**
     * 获取接口响应时间分位数
     *
     * @param className  类名
     * @param methodName 方法名
     * @param timeWindow 时间窗口
     * @return 响应时间统计 (p50, p95, p99)
     */
    ResponseTimeStats getResponseTimeStats(String className, String methodName, TimeWindow timeWindow);

    /**
     * 获取错误率趋势
     *
     * @param className   类名
     * @param methodName  方法名
     * @param timeWindow  时间窗口
     * @param pointCount  数据点数量
     * @return 错误率趋势数据
     */
    Map<LocalDateTime, Double> getErrorRateTrend(String className, String methodName, 
                                                     TimeWindow timeWindow, int pointCount);

    /**
     * 获取地域分布统计
     *
     * @param timeWindow 时间窗口
     * @return 地域分布 (省份 -> 访问量)
     */
    Map<String, Long> getGeoDistribution(TimeWindow timeWindow);

    /**
     * 获取终端分布统计
     *
     * @param timeWindow 时间窗口
     * @return 终端分布 (Web/App/小程序 -> 访问量)
     */
    Map<String, Long> getTerminalDistribution(TimeWindow timeWindow);

    /**
     * 响应时间统计
     */
    record ResponseTimeStats(
            String className,
            String methodName,
            long p50,  // 中位数
            long p95,  // 95分位
            long p99,  // 99分位
            long avg,  // 平均值
            long max,  // 最大值
            long min   // 最小值
    ) {}
}
