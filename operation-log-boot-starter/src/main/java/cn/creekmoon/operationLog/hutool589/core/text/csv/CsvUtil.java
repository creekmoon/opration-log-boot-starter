package cn.creekmoon.operationLog.hutool589.core.text.csv;

import cn.creekmoon.operationLog.hutool589.core.text.csv.CsvReadConfig;
import cn.creekmoon.operationLog.hutool589.core.text.csv.CsvReader;
import cn.creekmoon.operationLog.hutool589.core.text.csv.CsvWriteConfig;
import cn.creekmoon.operationLog.hutool589.core.text.csv.CsvWriter;

import java.io.File;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;

/**
 * CSV工具
 *
 * @author looly
 * @since 4.0.5
 */
public class CsvUtil {

    //----------------------------------------------------------------------------------------------------------- Reader

    /**
     * 获取CSV读取器，调用此方法创建的Reader须自行指定读取的资源
     *
     * @param config 配置, 允许为空.
     * @return {@link cn.creekmoon.operationLog.hutool589.core.text.csv.CsvReader}
     */
    public static cn.creekmoon.operationLog.hutool589.core.text.csv.CsvReader getReader(cn.creekmoon.operationLog.hutool589.core.text.csv.CsvReadConfig config) {
        return new cn.creekmoon.operationLog.hutool589.core.text.csv.CsvReader(config);
    }

    /**
     * 获取CSV读取器，调用此方法创建的Reader须自行指定读取的资源
     *
     * @return {@link cn.creekmoon.operationLog.hutool589.core.text.csv.CsvReader}
     */
    public static cn.creekmoon.operationLog.hutool589.core.text.csv.CsvReader getReader() {
        return new cn.creekmoon.operationLog.hutool589.core.text.csv.CsvReader();
    }

    /**
     * 获取CSV读取器
     *
     * @param reader {@link Reader}
     * @param config 配置, {@code null}表示默认配置
     * @return {@link cn.creekmoon.operationLog.hutool589.core.text.csv.CsvReader}
     * @since 5.7.14
     */
    public static cn.creekmoon.operationLog.hutool589.core.text.csv.CsvReader getReader(Reader reader, CsvReadConfig config) {
        return new cn.creekmoon.operationLog.hutool589.core.text.csv.CsvReader(reader, config);
    }

    /**
     * 获取CSV读取器
     *
     * @param reader {@link Reader}
     * @return {@link cn.creekmoon.operationLog.hutool589.core.text.csv.CsvReader}
     * @since 5.7.14
     */
    public static CsvReader getReader(Reader reader) {
        return getReader(reader, null);
    }

    //----------------------------------------------------------------------------------------------------------- Writer

    /**
     * 获取CSV生成器（写出器），使用默认配置，覆盖已有文件（如果存在）
     *
     * @param filePath File CSV文件路径
     * @param charset  编码
     * @return {@link cn.creekmoon.operationLog.hutool589.core.text.csv.CsvWriter}
     */
    public static cn.creekmoon.operationLog.hutool589.core.text.csv.CsvWriter getWriter(String filePath, Charset charset) {
        return new cn.creekmoon.operationLog.hutool589.core.text.csv.CsvWriter(filePath, charset);
    }

    /**
     * 获取CSV生成器（写出器），使用默认配置，覆盖已有文件（如果存在）
     *
     * @param file    File CSV文件
     * @param charset 编码
     * @return {@link cn.creekmoon.operationLog.hutool589.core.text.csv.CsvWriter}
     */
    public static cn.creekmoon.operationLog.hutool589.core.text.csv.CsvWriter getWriter(File file, Charset charset) {
        return new cn.creekmoon.operationLog.hutool589.core.text.csv.CsvWriter(file, charset);
    }

    /**
     * 获取CSV生成器（写出器），使用默认配置
     *
     * @param filePath File CSV文件路径
     * @param charset  编码
     * @param isAppend 是否追加
     * @return {@link cn.creekmoon.operationLog.hutool589.core.text.csv.CsvWriter}
     */
    public static cn.creekmoon.operationLog.hutool589.core.text.csv.CsvWriter getWriter(String filePath, Charset charset, boolean isAppend) {
        return new cn.creekmoon.operationLog.hutool589.core.text.csv.CsvWriter(filePath, charset, isAppend);
    }

    /**
     * 获取CSV生成器（写出器），使用默认配置
     *
     * @param file     File CSV文件
     * @param charset  编码
     * @param isAppend 是否追加
     * @return {@link cn.creekmoon.operationLog.hutool589.core.text.csv.CsvWriter}
     */
    public static cn.creekmoon.operationLog.hutool589.core.text.csv.CsvWriter getWriter(File file, Charset charset, boolean isAppend) {
        return new cn.creekmoon.operationLog.hutool589.core.text.csv.CsvWriter(file, charset, isAppend);
    }

    /**
     * 获取CSV生成器（写出器）
     *
     * @param file     File CSV文件
     * @param charset  编码
     * @param isAppend 是否追加
     * @param config   写出配置，null则使用默认配置
     * @return {@link cn.creekmoon.operationLog.hutool589.core.text.csv.CsvWriter}
     */
    public static cn.creekmoon.operationLog.hutool589.core.text.csv.CsvWriter getWriter(File file, Charset charset, boolean isAppend, cn.creekmoon.operationLog.hutool589.core.text.csv.CsvWriteConfig config) {
        return new cn.creekmoon.operationLog.hutool589.core.text.csv.CsvWriter(file, charset, isAppend, config);
    }

    /**
     * 获取CSV生成器（写出器）
     *
     * @param writer Writer
     * @return {@link cn.creekmoon.operationLog.hutool589.core.text.csv.CsvWriter}
     */
    public static cn.creekmoon.operationLog.hutool589.core.text.csv.CsvWriter getWriter(Writer writer) {
        return new cn.creekmoon.operationLog.hutool589.core.text.csv.CsvWriter(writer);
    }

    /**
     * 获取CSV生成器（写出器）
     *
     * @param writer Writer
     * @param config 写出配置，null则使用默认配置
     * @return {@link cn.creekmoon.operationLog.hutool589.core.text.csv.CsvWriter}
     */
    public static cn.creekmoon.operationLog.hutool589.core.text.csv.CsvWriter getWriter(Writer writer, CsvWriteConfig config) {
        return new CsvWriter(writer, config);
    }
}
