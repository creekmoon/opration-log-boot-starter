package cn.creekmoon.operationLog.dashboard;

import cn.creekmoon.operationLog.heatmap.HeatmapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Dashboard SSE 流控制器
 * 提供实时数据推送
 */
@Slf4j
@RestController
@RequestMapping("/operation-log/dashboard")
@RequiredArgsConstructor
public class DashboardStreamController {

    private final DashboardDataService dashboardDataService;
    private final HeatmapService heatmapService;

    // 存储所有活跃的 SSE 连接
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    /**
     * SSE 实时数据流端点
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        String emitterId = java.util.UUID.randomUUID().toString();
        SseEmitter emitter = new SseEmitter(0L); // 无超时

        emitters.put(emitterId, emitter);
        log.debug("SSE 连接建立: {}", emitterId);

        // 发送初始数据
        sendInitialData(emitter);

        // 设置定时推送
        ScheduledExecutorService emitterScheduler = Executors.newSingleThreadScheduledExecutor();
        emitterScheduler.scheduleAtFixedRate(() -> {
            try {
                sendDashboardUpdate(emitter);
            } catch (Exception e) {
                log.warn("发送 SSE 数据失败", e);
                emitter.completeWithError(e);
                emitterScheduler.shutdown();
            }
        }, 5, 5, TimeUnit.SECONDS); // 每 5 秒推送一次

        // 清理连接
        emitter.onCompletion(() -> {
            log.debug("SSE 连接完成: {}", emitterId);
            emitters.remove(emitterId);
            emitterScheduler.shutdown();
        });

        emitter.onTimeout(() -> {
            log.debug("SSE 连接超时: {}", emitterId);
            emitters.remove(emitterId);
            emitterScheduler.shutdown();
        });

        emitter.onError((e) -> {
            log.debug("SSE 连接错误: {}", emitterId, e);
            emitters.remove(emitterId);
            emitterScheduler.shutdown();
        });

        return emitter;
    }

    /**
     * 发送初始数据
     */
    private void sendInitialData(SseEmitter emitter) {
        try {
            DashboardDataService.DashboardRealtimeData data = dashboardDataService.getRealtimeData();

            SseEmitter.SseEventBuilder event = SseEmitter.event()
                .name("dashboard-update")
                .data(new StreamData(
                    data.totalPv(),
                    data.totalUv(),
                    data.activeEndpoints(),
                    LocalDateTime.now()
                ));

            emitter.send(event);
        } catch (IOException e) {
            log.warn("发送初始 SSE 数据失败", e);
        }
    }

    /**
     * 发送 Dashboard 更新
     */
    private void sendDashboardUpdate(SseEmitter emitter) throws IOException {
        DashboardDataService.DashboardRealtimeData data = dashboardDataService.getRealtimeData();

        SseEmitter.SseEventBuilder event = SseEmitter.event()
            .name("dashboard-update")
            .data(new StreamData(
                data.totalPv(),
                data.totalUv(),
                data.activeEndpoints(),
                LocalDateTime.now()
            ));

        emitter.send(event);
    }

    /**
     * SSE 数据 DTO
     */
    public record StreamData(
            long totalPv,
            long totalUv,
            int activeEndpoints,
            LocalDateTime timestamp
    ) {}
}
