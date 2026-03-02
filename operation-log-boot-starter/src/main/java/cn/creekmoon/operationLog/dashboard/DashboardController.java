package cn.creekmoon.operationLog.dashboard;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 操作日志可视化Dashboard控制器
 * 提供监控面板入口
 */
@Controller
@RequestMapping("/operation-log")
public class DashboardController {

    /**
     * Dashboard首页 (V3 - Grafana风格)
     */
    @GetMapping({"/dashboard", "/dashboard/"})
    public String dashboard() {
        return "forward:/operation-log-dashboard-v3.html";
    }

    /**
     * Dashboard V2 (旧版本)
     */
    @GetMapping("/dashboard/v2")
    public String dashboardV2() {
        return "forward:/operation-log-dashboard-v2.html";
    }

    /**
     * Dashboard V1 (旧版本)
     */
    @GetMapping("/dashboard/v1")
    public String dashboardV1() {
        return "forward:/operation-log-dashboard.html";
    }
}
