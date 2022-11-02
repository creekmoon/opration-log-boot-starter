package cn.creekmoon.operationLog.hutool589.core.lang.generator;

import cn.creekmoon.operationLog.hutool589.core.lang.ObjectId;
import cn.creekmoon.operationLog.hutool589.core.lang.generator.Generator;

/**
 * ObjectId生成器
 *
 * @author looly
 * @since 5.4.3
 */
public class ObjectIdGenerator implements Generator<String> {
    @Override
    public String next() {
        return ObjectId.next();
    }
}
