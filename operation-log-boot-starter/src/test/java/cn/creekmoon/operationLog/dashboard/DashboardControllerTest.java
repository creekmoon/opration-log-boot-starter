package cn.creekmoon.operationLog.dashboard;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Dashboard控制器测试类
 */
class DashboardControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        DashboardController controller = new DashboardController();
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void testDashboard() throws Exception {
        mockMvc.perform(get("/operation-log/dashboard"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/operation-log-dashboard-v3.html"));
    }

    @Test
    void testDashboardWithTrailingSlash() throws Exception {
        mockMvc.perform(get("/operation-log/dashboard/"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/operation-log-dashboard-v3.html"));
    }

    @Test
    void testDashboardV2() throws Exception {
        mockMvc.perform(get("/operation-log/dashboard/v2"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/operation-log-dashboard-v2.html"));
    }

    @Test
    void testDashboardV1() throws Exception {
        mockMvc.perform(get("/operation-log/dashboard/v1"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/operation-log-dashboard.html"));
    }
}
