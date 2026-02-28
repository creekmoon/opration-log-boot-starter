package cn.creekmoon.operationLog.profile;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 默认标签策略
 * 基于用户行为统计自动生成标签
 */
@Component
public class DefaultTagStrategy {

    /**
     * 生成用户标签
     *
     * @param userStats 用户统计信息
     * @return 标签集合
     */
    public Set<String> generateTags(UserStats userStats) {
        Set<String> tags = new HashSet<>();

        // 1. 基于操作频率的标签
        generateFrequencyTags(userStats, tags);

        // 2. 基于用户生命周期的标签
        generateLifecycleTags(userStats, tags);

        // 3. 基于操作类型的标签
        generateOperationTypeTags(userStats, tags);

        // 4. 基于活跃时间的标签
        generateActiveTimeTags(userStats, tags);

        return tags;
    }

    /**
     * 频率标签
     */
    private void generateFrequencyTags(UserStats stats, Set<String> tags) {
        long count7d = stats.getLast7DaysCount();
        long count30d = stats.getLast30DaysCount();

        // 高频用户：7天内操作 > 50次
        if (count7d > 50) {
            tags.add("高频用户");
        }
        // 活跃用户：7天内操作 > 10次
        else if (count7d > 10) {
            tags.add("活跃用户");
        }
        // 低频用户：7天内操作 < 3次
        else if (count7d < 3 && count7d > 0) {
            tags.add("低频用户");
        }

        // 深度用户：30天内操作 > 200次
        if (count30d > 200) {
            tags.add("深度用户");
        }
    }

    /**
     * 生命周期标签
     */
    private void generateLifecycleTags(UserStats stats, Set<String> tags) {
        LocalDateTime firstActive = stats.getFirstActiveTime();
        LocalDateTime lastActive = stats.getLastActiveTime();
        LocalDateTime now = LocalDateTime.now();

        if (firstActive != null) {
            // 新用户：首次活跃时间在7天内
            if (firstActive.isAfter(now.minusDays(7))) {
                tags.add("新用户");
            }
            // 老用户：首次活跃时间超过90天
            else if (firstActive.isBefore(now.minusDays(90))) {
                tags.add("老用户");
            }
        }

        if (lastActive != null) {
            // 沉默用户：30天未活跃
            if (lastActive.isBefore(now.minusDays(30))) {
                tags.add("沉默用户");
            }
            // 流失风险：7天未活跃但30天内活跃过
            else if (lastActive.isBefore(now.minusDays(7)) 
                    && lastActive.isAfter(now.minusDays(30))) {
                tags.add("流失风险");
            }
        }
    }

    /**
     * 操作类型标签
     */
    private void generateOperationTypeTags(UserStats stats, Set<String> tags) {
        Map<String, Long> typeStats = stats.getOperationTypeStats();
        long total = typeStats.values().stream().mapToLong(Long::longValue).sum();

        if (total == 0) {
            return;
        }

        // 查询型用户：查询操作占比 > 80%
        long queryCount = getTypeCount(typeStats, "QUERY", "LIST", "GET", "SEARCH");
        if (queryCount * 100 / total > 80) {
            tags.add("查询型用户");
        }

        // 提交型用户：创建/提交操作占比 > 30%
        long submitCount = getTypeCount(typeStats, "CREATE", "SUBMIT", "SAVE");
        if (submitCount * 100 / total > 30) {
            tags.add("提交型用户");
        }

        // 管理型用户：更新/删除操作占比 > 20%
        long manageCount = getTypeCount(typeStats, "UPDATE", "DELETE", "EDIT");
        if (manageCount * 100 / total > 20) {
            tags.add("管理型用户");
        }

        // 导入导出用户
        long ioCount = getTypeCount(typeStats, "IMPORT", "EXPORT", "UPLOAD", "DOWNLOAD");
        if (ioCount > 0) {
            tags.add("数据用户");
        }
    }

    /**
     * 活跃时间标签
     */
    private void generateActiveTimeTags(UserStats stats, Set<String> tags) {
        Map<Integer, Long> hourDistribution = stats.getHourDistribution();
        long total = hourDistribution.values().stream().mapToLong(Long::longValue).sum();

        if (total == 0) {
            return;
        }

        // 计算各时段操作数
        long nightCount = 0;    // 22:00 - 06:00
        long workCount = 0;     // 09:00 - 18:00
        long weekendCount = stats.getWeekendCount();

        for (Map.Entry<Integer, Long> entry : hourDistribution.entrySet()) {
            int hour = entry.getKey();
            long count = entry.getValue();

            if (hour >= 22 || hour < 6) {
                nightCount += count;
            }
            if (hour >= 9 && hour < 18) {
                workCount += count;
            }
        }

        // 夜猫子：夜间操作占比 > 50%
        if (nightCount * 100 / total > 50) {
            tags.add("夜猫子");
        }

        // 工作时间用户：工作时间操作占比 > 70%
        if (workCount * 100 / total > 70) {
            tags.add("工作时间用户");
        }

        // 周末用户：周末操作占比 > 30%
        if (weekendCount * 100 / total > 30) {
            tags.add("周末用户");
        }
    }

    /**
     * 获取多个类型的总计数
     */
    private long getTypeCount(Map<String, Long> typeStats, String... types) {
        long count = 0;
        for (String type : types) {
            count += typeStats.getOrDefault(type, 0L);
        }
        return count;
    }

    /**
     * 用户统计信息接口
     */
    public interface UserStats {
        long getLast7DaysCount();
        long getLast30DaysCount();
        LocalDateTime getFirstActiveTime();
        LocalDateTime getLastActiveTime();
        Map<String, Long> getOperationTypeStats();
        Map<Integer, Long> getHourDistribution();
        long getWeekendCount();
    }
}
