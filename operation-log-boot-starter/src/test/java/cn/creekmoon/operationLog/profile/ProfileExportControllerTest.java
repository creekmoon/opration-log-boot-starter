package cn.creekmoon.operationLog.profile;

import cn.creekmoon.operationLog.export.CsvExportService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 用户画像导出Controller测试类
 */
@ExtendWith(MockitoExtension.class)
class ProfileExportControllerTest {

    @Mock
    private ProfileService profileService;

    @Mock
    private CsvExportService csvExportService;

    @Mock
    private HttpServletResponse response;

    private ProfileExportController controller;

    @BeforeEach
    void setUp() {
        controller = new ProfileExportController(profileService, csvExportService);
    }

    @Test
    void testExportUserProfile() throws IOException {
        // Given
        String userId = "user123";
        List<List<String>> mockData = Arrays.asList(
                Arrays.asList("用户ID", "标签", "操作类型", "操作次数"),
                Arrays.asList("user123", "高频查询用户", "ORDER_QUERY", "100")
        );
        when(profileService.exportUserProfileToCsv(userId)).thenReturn(mockData);
        when(csvExportService.generateFileName(anyString())).thenReturn("profile-user-user123-20240228-120000.csv");
        when(csvExportService.getContentType()).thenReturn("text/csv;charset=UTF-8");

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        when(response.getOutputStream()).thenReturn(new ServletOutputStreamWrapper(outputStream));

        // When
        controller.exportUserProfile(userId, response);

        // Then
        verify(profileService).exportUserProfileToCsv(userId);
        verify(csvExportService).generateFileName("profile-user-" + userId);
        verify(response).setContentType("text/csv;charset=UTF-8");
    }

    @Test
    void testExportUsersByTag() throws IOException {
        // Given
        String tag = "高频查询用户";
        List<List<String>> mockData = Arrays.asList(
                Arrays.asList("用户ID", "标签", "ORDER_QUERY次数", "ORDER_SUBMIT次数", "ORDER_REFUND次数"),
                Arrays.asList("user123", "高频查询用户", "100", "10", "0")
        );
        when(profileService.exportUsersByTagToCsv(tag, 0, 100)).thenReturn(mockData);
        when(csvExportService.generateFileName(anyString())).thenReturn("profile-tag-高频查询用户-20240228-120000.csv");

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        when(response.getOutputStream()).thenReturn(new ServletOutputStreamWrapper(outputStream));

        // When
        controller.exportUsersByTag(tag, 0, 100, response);

        // Then
        verify(profileService).exportUsersByTagToCsv(tag, 0, 100);
    }

    @Test
    void testExportAllUsers() throws IOException {
        // Given
        List<List<String>> mockData = Arrays.asList(
                Arrays.asList("用户ID", "标签列表", "操作统计JSON"),
                Arrays.asList("user123", "高频查询用户;高价值用户", "{ORDER_QUERY=100}")
        );
        when(profileService.exportAllUserStatsToCsv(1000)).thenReturn(mockData);
        when(csvExportService.generateFileName(anyString())).thenReturn("profile-all-users-20240228-120000.csv");

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        when(response.getOutputStream()).thenReturn(new ServletOutputStreamWrapper(outputStream));

        // When
        controller.exportAllUsers(1000, response);

        // Then
        verify(profileService).exportAllUserStatsToCsv(1000);
    }

    /**
     * 简单的ServletOutputStream包装类用于测试
     */
    static class ServletOutputStreamWrapper extends jakarta.servlet.ServletOutputStream {
        private final ByteArrayOutputStream outputStream;

        public ServletOutputStreamWrapper(ByteArrayOutputStream outputStream) {
            this.outputStream = outputStream;
        }

        @Override
        public void write(int b) throws IOException {
            outputStream.write(b);
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setWriteListener(jakarta.servlet.WriteListener writeListener) {
        }
    }
}
