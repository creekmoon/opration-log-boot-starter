package cn.creekmoon.operationLog.export;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.function.Function;

/**
 * CSV导出服务接口
 */
public interface CsvExportService {

    /**
     * 导出数据为CSV格式
     *
     * @param headers    CSV表头
     * @param data       数据列表
     * @param rowMapper  数据行映射函数
     * @param outputStream 输出流
     * @param <T>        数据类型
     * @throws IOException IO异常
     */
    <T> void exportCsv(List<String> headers, List<T> data, Function<T, List<String>> rowMapper,
                     OutputStream outputStream) throws IOException;

    /**
     * 导出数据为CSV格式（带BOM，Excel兼容）
     *
     * @param headers    CSV表头
     * @param data       数据列表
     * @param rowMapper  数据行映射函数
     * @param outputStream 输出流
     * @param <T>        数据类型
     * @throws IOException IO异常
     */
    <T> void exportCsvWithBom(List<String> headers, List<T> data, Function<T, List<String>> rowMapper,
                            OutputStream outputStream) throws IOException;

    /**
     * 生成CSV文件名
     *
     * @param prefix 文件名前缀
     * @return 带时间戳的文件名
     */
    String generateFileName(String prefix);

    /**
     * 获取CSV内容类型
     *
     * @return MIME类型
     */
    String getContentType();
}
