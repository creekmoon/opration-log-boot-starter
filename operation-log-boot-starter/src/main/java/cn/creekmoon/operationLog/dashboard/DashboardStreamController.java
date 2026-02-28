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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Dashboard 实时数据流控制器
 * 使用 SSE (Server-Sent Events) 推送实时数据
 */
@Slf4j
@RestController
@RequestMapping("/operation-log/dashboard")
@RequiredArgsConstructor
public class DashboardStreamController {

    private final HeatmapService heatmapService;
    private final DashboardDataService dashboardDataService;
    
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    /**
     * SSE 实时数据流
     * 每 5 秒推送一次 Dashboard 数据
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamDashboardData() {
        SseEmitter emitter = new SseEmitter(0L); // 无超时
        
        // 发送连接成功事件
        try {
            emitter.send(SseEmitter.event()
                .name("connected")
                .data("{\"status\":\"connected\",\"interval\":5}"));
        } catch (IOException e) {
            log.error("SSE 连接初始化失败", e);
            emitter.completeWithError(e);
            return emitter;
        }
        
        // 定期推送数据
        var future = scheduler.scheduleAtFixedRate(() -> {
            try {
                DashboardRealtimeData data = dashboardDataService.getRealtimeData();
                emitter.send(SseEmitter.event()
                    .name("dashboard-update")
                    .data(data));
            } catch (IOException e) {
                log.warn("SSE 数据发送失败，连接可能已关闭");
                emitter.complete();
            }
        }, 0, 5, TimeUnit.SECONDS);
        
        // 连接关闭时清理资源
        emitter.onCompletion(() -> {
            future.cancel(false);
            log.debug("SSE 连接已关闭");
        });
        
        emitter.onTimeout(() -> {
            future.cancel(false);
            log.debug("SSE 连接超时");
        });
        
        emitter.onError((e) -> {
            future.cancel(false);
            log.error("SSE 连接错误", e);
        });
        
        return emitter;
    }
    
    /**
     * 手动刷新数据接口
     */
    @GetMapping("/api/realtime")
    public DashboardRealtimeData getRealtimeData() {
        return dashboardDataService.getRealtimeData();
    }
}
