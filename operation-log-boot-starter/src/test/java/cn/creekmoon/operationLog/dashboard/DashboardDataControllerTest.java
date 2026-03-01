package cn.creekmoon.operationLog.dashboard;

import cn.creekmoon.operationLog.heatmap.HeatmapService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Dashboard 数据控制器测试类
 */
class DashboardDataControllerTest {

    private MockMvc mockMvc;
    private HeatmapService heatmapService;
    private DashboardDataService dashboardDataService;

    @BeforeEach
    void setUp() {
        heatmapService = mock(HeatmapService.class);
        dashboardDataService = new DashboardDataService(heatmapService);
        DashboardDataController controller = new DashboardDataController(heatmapService, dashboardDataService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void testGetOverviewData() throws Exception {
        // 准备 mock 数据
        when(heatmapService.getAllRealtimeStats()).thenReturn(Map.of());

        mockMvc.perform(get("/operation-log/dashboard/api/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOperations").exists())
                .andExpect(jsonPath("$.totalUsers").exists())
                .andExpect(jsonPath("$.todayOperations").exists())
                .andExpect(jsonPath("$.avgResponseTime").exists())
                .andExpect(jsonPath("$.errorRate").exists())
                .andExpect(jsonPath("$.trend").exists());
    }

    @Test
    void testGetOverviewDataWithDateRange() throws Exception {
        when(heatmapService.getAllRealtimeStats()).thenReturn(Map.of());

        mockMvc.perform(get("/operation-log/dashboard/api/overview")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOperations").exists())
                .andExpect(jsonPath("$.totalUsers").exists());
    }

    @Test
    void testGetTopOperations() throws Exception {
        when(heatmapService.getTopN(any(), any(), anyInt())).thenReturn(List.of(
                new HeatmapService.HeatmapTopItem(1, "TestController", "testMethod", 100L, HeatmapService.MetricType.PV)
        ));

        mockMvc.perform(get("/operation-log/dashboard/api/top-operations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].operationName").exists())
                .andExpect(jsonPath("$[0].count").value(100));
    }

    @Test
    void testGetTopOperationsWithLimit() throws Exception {
        when(heatmapService.getTopN(any(), any(), anyInt())).thenReturn(List.of());

        mockMvc.perform(get("/operation-log/dashboard/api/top-operations")
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void testGetTrendData() throws Exception {
        mockMvc.perform(get("/operation-log/dashboard/api/trend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void testGetTrendDataWithDateRange() throws Exception {
        mockMvc.perform(get("/operation-log/dashboard/api/trend")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void testGetOperationTypeDistribution() throws Exception {
        mockMvc.perform(get("/operation-log/dashboard/api/operation-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isMap());
    }

    @Test
    void testGetOperationTypeDistributionWithDateRange() throws Exception {
        mockMvc.perform(get("/operation-log/dashboard/api/operation-types")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isMap());
    }
}
