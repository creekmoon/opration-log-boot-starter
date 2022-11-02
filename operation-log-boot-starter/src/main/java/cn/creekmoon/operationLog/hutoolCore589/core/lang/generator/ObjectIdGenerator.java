package cn.creekmoon.operationLog.hutoolCore589.core.lang.generator;

import cn.hutool.core.lang.ObjectId;
import cn.hutool.core.lang.generator.Generator;

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
