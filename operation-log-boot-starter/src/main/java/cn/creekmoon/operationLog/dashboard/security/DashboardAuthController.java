package cn.creekmoon.operationLog.dashboard.security;

import cn.creekmoon.operationLog.dashboard.DashboardProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dashboard 认证控制器（已废弃）
 * 
 * v2.3+ 变更说明：
 * - Basic Auth 不需要额外端点，浏览器会自动处理认证对话框
 * - 此类保留用于向后兼容，但所有方法返回废弃警告
 * - v3.0 将移除此类
 * 
 * @deprecated v2.3+ 使用 Basic Auth，此类将在 v3.0 移除
 */
@Slf4j
@RestController
@RequestMapping("/operation-log/dashboard/auth")
@RequiredArgsConstructor
@Deprecated(since = "2.3", forRemoval = true)
public class DashboardAuthController {

    private final DashboardSecurityService securityService;
    private final DashboardProperties properties;

    /**
     * 获取当前安全配置状态
     * 
     * @deprecated v2.3+ 使用 Basic Auth，无需状态查询
     */
    @GetMapping("/status")
    @Deprecated(since = "2.3", forRemoval = true)
    public ResponseEntity<Map<String, Object>> getAuthStatus() {
        log.warn("[Deprecated] /operation-log/dashboard/auth/status 端点已废弃，v3.0 将移除");
        
        Map<String, Object> status = new HashMap<>();
        status.put("enabled", properties.isEnabled());
        status.put("deprecated", true);
        status.put("message", "此端点已废弃，v2.3+ 使用 Basic Auth");
        
        // 返回新配置信息
        if (properties.isAuthEnabled()) {
            status.put("authType", "Basic Auth");
            status.put("authConfigured", true);
        } else {
            status.put("authType", "None");
            status.put("authConfigured", false);
        }

        return ResponseEntity.ok(status);
    }

    /**
     * 验证Token有效性
     * 
     * @deprecated v2.3+ 使用 Basic Auth
     */
    @PostMapping("/verify")
    @Deprecated(since = "2.3", forRemoval = true)
    public ResponseEntity<Map<String, Object>> verifyToken(HttpServletRequest request) {
        log.warn("[Deprecated] /operation-log/dashboard/auth/verify 端点已废弃，v2.3+ 使用 Basic Auth");
        
        Map<String, Object> result = new HashMap<>();
        result.put("valid", false);
        result.put("deprecated", true);
        result.put("message", "Token 认证已废弃，请使用 Basic Auth");
        
        return ResponseEntity.status(410).body(result); // 410 Gone
    }

    /**
     * 获取当前客户端IP
     * 
     * @deprecated v2.3+ IP 白名单功能已废弃
     */
    @GetMapping("/client-ip")
    @Deprecated(since = "2.3", forRemoval = true)
    public ResponseEntity<Map<String, Object>> getClientIp(HttpServletRequest request) {
        log.warn("[Deprecated] /operation-log/dashboard/auth/client-ip 端点已废弃，v3.0 将移除");
        
        Map<String, Object> result = new HashMap<>();
        String clientIp = securityService.getClientIp(request);
        
        result.put("clientIp", clientIp);
        result.put("deprecated", true);
        result.put("message", "IP 白名单功能已废弃，v2.3+ 使用 Basic Auth");
        
        return ResponseEntity.ok(result);
    }
}
