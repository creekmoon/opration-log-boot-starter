package cn.jy.operationLog.core;

import cn.hutool.core.bean.BeanUtil;
import cn.jy.operationLog.utils.ObjectUtils;

import java.util.*;

/**
 * 实现这个接口,需要保证线程安全!
 */
public interface OperationLogHandler {


    /**
     * 定义如何保存这个日志
     *
     * @param logRecord
     */
    void save(LogRecord logRecord);


}
