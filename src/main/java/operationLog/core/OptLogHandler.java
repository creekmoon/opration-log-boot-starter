package operationLog.core;

public interface OptLogHandler {


    /**
     * 定义http本次请求是否成功  如果成功则记录 不成功则不记录
     *
     * @param functionResult
     * @return
     */
    boolean requestIsSuccess(Object functionResult);


    /**
     * 定义如何保存这个日志
     *
     * @param LogRecord
     */
    void save(LogRecord LogRecord);
}
