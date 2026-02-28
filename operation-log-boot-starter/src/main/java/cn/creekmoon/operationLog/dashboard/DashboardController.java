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
     * Dashboard首页
     */
    @GetMapping({"/dashboard", "/dashboard/"})
    public String dashboard() {
        return "forward:/operation-log-dashboard.html";
    }
}
