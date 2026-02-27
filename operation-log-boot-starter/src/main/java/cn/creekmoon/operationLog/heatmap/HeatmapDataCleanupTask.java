package cn.creekmoon.operationLog.heatmap;

import cn.creekmoon.operationLog.profile.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 热力图和用户画像数据清理任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HeatmapDataCleanupTask {

    private final HeatmapService heatmapService;
    private final ProfileService profileService;

    /**
     * 每小时执行一次数据清理
     */
    @Scheduled(cron = "0 0 * * * *")
    public void cleanup() {
        log.info("[operation-log] Starting scheduled data cleanup");
        try {
            heatmapService.cleanupExpiredData();
        } catch (Exception e) {
            log.error("[operation-log] Heatmap cleanup error", e);
        }
        try {
            profileService.cleanupExpiredData();
        } catch (Exception e) {
            log.error("[operation-log] Profile cleanup error", e);
        }
        log.info("[operation-log] Scheduled data cleanup completed");
    }

    /**
     * 每天凌晨2点刷新所有用户标签
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void refreshTags() {
        log.info("[operation-log] Starting scheduled tag refresh");
        try {
            profileService.refreshAllUserTags();
        } catch (Exception e) {
            log.error("[operation-log] Tag refresh error", e);
        }
        log.info("[operation-log] Scheduled tag refresh completed");
    }
}
