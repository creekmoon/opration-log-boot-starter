package cn.creekmoon.operationLog.heatmap;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 热力图扩展服务接口
 * 
 * 提供热力图的高级统计功能，与基础HeatmapService通过组合方式协作。
 * 本接口只定义扩展功能，不继承基础接口，避免接口膨胀。
 * 
 * @author creekmoon
 * @since 2.2.0
 */
public interface HeatmapExtensionService {

    /**
     * 获取响应时间统计
     * 
     * @param className 类名
     * @param methodName 方法名  
     * @param timeWindow 时间窗口
     * @return 响应时间统计对象
     */
    ResponseTimeStats getResponseTimeStats(String className, String methodName, 
                                            HeatmapService.TimeWindow timeWindow);

    /**
     * 获取错误率趋势
     * 
     * @param className 类名
     * @param methodName 方法名
     * @param timeWindow 时间窗口
     * @param pointCount 数据点数量
     * @return 时间戳到错误率的映射
     */
    Map<LocalDateTime, Double> getErrorRateTrend(String className, String methodName,
                                                   HeatmapService.TimeWindow timeWindow, 
                                                   int pointCount);

    /**
     * 获取地域分布统计
     * 
     * @param timeWindow 时间窗口
     * @return 地域到访问量的映射
     */
    Map<String, Long> getGeoDistribution(HeatmapService.TimeWindow timeWindow);

    /**
     * 获取终端分布统计
     * 
     * @param timeWindow 时间窗口
     * @return 终端类型到访问量的映射
     */
    Map<String, Long> getTerminalDistribution(HeatmapService.TimeWindow timeWindow);

    /**
     * 响应时间统计记录
     * 
     * @param className 类名
     * @param methodName 方法名
     * @param p50 P50分位数
     * @param p95 P95分位数
     * @param p99 P99分位数
     * @param avg 平均值
     * @param max 最大值
     * @param min 最小值
     */
    record ResponseTimeStats(
            String className,
            String methodName,
            long p50,
            long p95,
            long p99,
            long avg,
            long max,
            long min
    ) {
        /**
         * 创建空的统计对象
         */
        public static ResponseTimeStats empty(String className, String methodName) {
            return new ResponseTimeStats(className, methodName, 0, 0, 0, 0, 0, 0);
        }
    }
}
