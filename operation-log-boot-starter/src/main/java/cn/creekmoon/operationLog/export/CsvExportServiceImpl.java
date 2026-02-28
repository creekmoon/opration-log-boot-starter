package cn.creekmoon.operationLog.export;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;

/**
 * CSV导出服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CsvExportServiceImpl implements CsvExportService {

    private static final DateTimeFormatter FILE_NAME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final String CSV_CONTENT_TYPE = "text/csv;charset=UTF-8";
    
    /* UTF-8 BOM头，用于Excel兼容 */
    private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    @Override
    public <T> void exportCsv(List<String> headers, List<T> data, Function<T, List<String>> rowMapper,
                            OutputStream outputStream) throws IOException {
        exportCsvInternal(headers, data, rowMapper, outputStream, false);
    }

    @Override
    public <T> void exportCsvWithBom(List<String> headers, List<T> data, Function<T, List<String>> rowMapper,
                                   OutputStream outputStream) throws IOException {
        exportCsvInternal(headers, data, rowMapper, outputStream, true);
    }

    private <T> void exportCsvInternal(List<String> headers, List<T> data, Function<T, List<String>> rowMapper,
                                       OutputStream outputStream, boolean withBom) throws IOException {
        /* 写入BOM头（Excel兼容模式） */
        if (withBom) {
            outputStream.write(UTF8_BOM);
        }

        /* 构建CSV并写入数据 */
        try (Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                     .setHeader(headers.toArray(new String[0]))
                     .build())) {

            for (T item : data) {
                List<String> row = rowMapper.apply(item);
                csvPrinter.printRecord(row);
            }

            csvPrinter.flush();
        }
    }

    @Override
    public String generateFileName(String prefix) {
        String timestamp = LocalDateTime.now().format(FILE_NAME_FORMATTER);
        return String.format("%s-%s.csv", prefix, timestamp);
    }

    @Override
    public String getContentType() {
        return CSV_CONTENT_TYPE;
    }
}
