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
                .andExpect(forwardedUrl("/operation-log-dashboard.html"));
    }

    @Test
    void testDashboardWithTrailingSlash() throws Exception {
        mockMvc.perform(get("/operation-log/dashboard/"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/operation-log-dashboard.html"));
    }
}
