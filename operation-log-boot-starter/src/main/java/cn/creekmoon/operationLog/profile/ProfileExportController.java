package cn.creekmoon.operationLog.profile;

import cn.creekmoon.operationLog.export.CsvExportService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * 用户画像数据导出Controller
 * 提供CSV导出接口
 */
@RestController
@RequestMapping("/operation-log/profile")
@RequiredArgsConstructor
public class ProfileExportController {

    private final ProfileService profileService;
    private final CsvExportService csvExportService;

    /**
     * 导出指定用户的画像为CSV
     */
    @GetMapping("/export/user/{userId}")
    public void exportUserProfile(@PathVariable String userId, HttpServletResponse response) throws IOException {
        List<List<String>> data = profileService.exportUserProfileToCsv(userId);
        
        String fileName = csvExportService.generateFileName("profile-user-" + userId);
        response.setContentType(csvExportService.getContentType());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, 
                "attachment; filename=\"" + fileName + "\"");
        
        try (OutputStream out = response.getOutputStream()) {
            csvExportService.exportCsvWithBom(data.get(0), data.subList(1, data.size()), 
                    row -> row, out);
        }
    }

    /**
     * 导出指定标签的用户列表为CSV
     */
    @GetMapping("/export/tag/{tag}")
    public void exportUsersByTag(
            @PathVariable String tag,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            HttpServletResponse response) throws IOException {
        
        List<List<String>> data = profileService.exportUsersByTagToCsv(tag, page, size);
        
        String fileName = csvExportService.generateFileName("profile-tag-" + tag);
        response.setContentType(csvExportService.getContentType());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, 
                "attachment; filename=\"" + fileName + "\"");
        
        try (OutputStream out = response.getOutputStream()) {
            csvExportService.exportCsvWithBom(data.get(0), data.subList(1, data.size()), 
                    row -> row, out);
        }
    }

    /**
     * 导出所有用户统计为CSV
     */
    @GetMapping("/export/all")
    public void exportAllUsers(
            @RequestParam(defaultValue = "1000") int limit,
            HttpServletResponse response) throws IOException {
        
        List<List<String>> data = profileService.exportAllUserStatsToCsv(limit);
        
        String fileName = csvExportService.generateFileName("profile-all-users");
        response.setContentType(csvExportService.getContentType());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, 
                "attachment; filename=\"" + fileName + "\"");
        
        try (OutputStream out = response.getOutputStream()) {
            csvExportService.exportCsvWithBom(data.get(0), data.subList(1, data.size()), 
                    row -> row, out);
        }
    }
}
