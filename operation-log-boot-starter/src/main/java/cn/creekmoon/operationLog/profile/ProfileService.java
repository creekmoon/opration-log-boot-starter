package cn.creekmoon.operationLog.profile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 用户行为画像服务接口
 * 提供用户操作统计、标签生成等功能
 */
public interface ProfileService {

    /**
     * 记录用户操作
     *
     * @param userId        用户ID
     * @param operationType 操作类型
     * @param timestamp     操作时间
     */
    void recordOperation(String userId, String operationType, LocalDateTime timestamp);

    /**
     * 记录用户操作(使用当前时间)
     *
     * @param userId        用户ID
     * @param operationType 操作类型
     */
    void recordOperation(String userId, String operationType);

    /**
     * 获取用户操作统计
     *
     * @param userId 用户ID
     * @return 操作类型到次数的映射
     */
    Map<String, Long> getUserOperationStats(String userId);

    /**
     * 获取用户操作统计(指定时间范围)
     *
     * @param userId 用户ID
     * @param days   最近多少天
     * @return 操作类型到次数的映射
     */
    Map<String, Long> getUserOperationStats(String userId, int days);

    /**
     * 获取用户标签列表
     *
     * @param userId 用户ID
     * @return 标签列表
     */
    Set<String> getUserTags(String userId);

    /**
     * 获取用户完整画像
     *
     * @param userId 用户ID
     * @return 用户画像
     */
    UserProfile getUserProfile(String userId);

    /**
     * 获取用户完整画像(指定时间范围)
     *
     * @param userId 用户ID
     * @param days   最近多少天
     * @return 用户画像
     */
    UserProfile getUserProfile(String userId, int days);

    /**
     * 根据标签查询用户列表
     *
     * @param tag  标签名称
     * @param page 页码(从0开始)
     * @param size 每页大小
     * @return 用户ID列表
     */
    List<String> getUsersByTag(String tag, int page, int size);

    /**
     * 根据标签查询用户数量
     *
     * @param tag 标签名称
     * @return 用户数量
     */
    long getUserCountByTag(String tag);

    /**
     * 刷新用户标签
     *
     * @param userId 用户ID
     */
    void refreshUserTags(String userId);

    /**
     * 批量刷新所有用户标签
     */
    void refreshAllUserTags();

    /**
     * 清理过期数据
     */
    void cleanupExpiredData();

    /**
     * 获取服务状态信息
     *
     * @return 状态信息
     */
    ProfileStatus getStatus();

    // ==================== CSV导出方法 ====================

    /**
     * 导出用户画像为CSV格式
     *
     * @param userId 用户ID
     * @return CSV数据行列表（包含表头）
     */
    List<List<String>> exportUserProfileToCsv(String userId);

    /**
     * 导出指定标签的用户列表为CSV格式
     *
     * @param tag    标签名称
     * @param page   页码
     * @param size   每页大小
     * @return CSV数据行列表（包含表头）
     */
    List<List<String>> exportUsersByTagToCsv(String tag, int page, int size);

    /**
     * 导出所有用户统计为CSV格式
     *
     * @param limit 最大导出数量
     * @return CSV数据行列表（包含表头）
     */
    List<List<String>> exportAllUserStatsToCsv(int limit);

    /**
     * 用户画像
     */
    record UserProfile(
            String userId,
            Set<String> tags,
            Map<String, Long> operationStats,
            Map<String, Long> last7DaysStats,
            Map<String, Long> last30DaysStats,
            LocalDateTime lastActiveTime,
            LocalDateTime profileGeneratedTime
    ) {}

    /**
     * 服务状态
     */
    record ProfileStatus(
            boolean enabled,
            boolean redisConnected,
            boolean tagEngineEnabled,
            boolean fallbackActive,
            long totalUsers,
            long totalTags
    ) {}
}
