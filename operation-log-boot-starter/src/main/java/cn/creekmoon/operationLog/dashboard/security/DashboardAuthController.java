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
 * Dashboard 认证控制器
 * 提供安全配置查询和Token验证接口
 */
@Slf4j
@RestController
@RequestMapping("/operation-log/dashboard/auth")
@RequiredArgsConstructor
public class DashboardAuthController {

    private final DashboardSecurityService securityService;
    private final DashboardProperties properties;

    /**
     * 获取当前安全配置状态
     * 用于前端初始化时了解认证要求
     *
     * @return 安全配置信息（敏感信息脱敏）
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getAuthStatus() {
        Map<String, Object> status = new HashMap<>();

        // Dashboard是否启用
        status.put("enabled", properties.isEnabled());

        // 当前认证模式
        DashboardProperties.AuthMode mode = properties.getAuthMode();
        status.put("mode", mode.name());

        // 各认证方式是否需要
        Map<String, Boolean> requirements = new HashMap<>();
        requirements.put("ipCheck", mode == DashboardProperties.AuthMode.IP_ONLY ||
                mode == DashboardProperties.AuthMode.IP_AND_TOKEN);
        requirements.put("tokenCheck", mode == DashboardProperties.AuthMode.TOKEN_ONLY ||
                mode == DashboardProperties.AuthMode.IP_AND_TOKEN);
        status.put("requirements", requirements);

        // IP白名单信息（仅显示数量，不显示具体IP）
        List<String> allowedIps = properties.getAllowIps();
        status.put("ipWhitelistCount", allowedIps != null ? allowedIps.size() : 0);

        // Token配置信息（仅显示是否配置）
        status.put("tokenConfigured", properties.getAuthToken() != null && !properties.getAuthToken().isEmpty());

        // Token传递方式
        status.put("tokenHeader", properties.getTokenHeader());
        status.put("allowTokenInQuery", properties.isAllowTokenInQuery());

        return ResponseEntity.ok(status);
    }

    /**
     * 验证Token有效性
     * 用于前端预检Token是否正确
     *
     * @param request HTTP请求
     * @return 验证结果
     */
    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyToken(HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();

        String token = securityService.extractToken(request);

        if (!securityService.checkTokenValid(token)) {
            result.put("valid", false);
            result.put("message", "Token无效或缺失");
            return ResponseEntity.status(401).body(result);
        }

        result.put("valid", true);
        result.put("message", "Token验证通过");
        return ResponseEntity.ok(result);
    }

    /**
     * 获取当前客户端IP
     * 用于调试IP白名单配置
     *
     * @param request HTTP请求
     * @return 客户端IP信息
     */
    @GetMapping("/client-ip")
    public ResponseEntity<Map<String, Object>> getClientIp(HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();

        String clientIp = securityService.getClientIp(request);
        boolean ipAllowed = securityService.checkIpAllowed(clientIp);

        result.put("clientIp", clientIp);
        result.put("ipAllowed", ipAllowed);

        // 当前白名单配置
        result.put("whitelist", properties.getAllowIps());

        return ResponseEntity.ok(result);
    }
}
