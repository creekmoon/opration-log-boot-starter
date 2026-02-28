package cn.creekmoon.operationLog.export;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * CSV导出配置属性
 */
@Data
@ConfigurationProperties(prefix = "operation-log.export.csv")
public class CsvExportProperties {

    /**
     * 是否启用CSV导出功能
     */
    private boolean enabled = true;

    /**
     * 是否带BOM（Excel兼容模式）
     */
    private boolean withBom = true;

    /**
     * CSV分隔符
     */
    private char delimiter = ',';

    /**
     * 单次导出最大行数限制（防止内存溢出）
     */
    private int maxExportRows = 10000;

    /**
     * 文件名前缀
     */
    private String fileNamePrefix = "export";
}
