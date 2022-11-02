package cn.creekmoon.operationLog.hutool589.core.lang.intern;

import cn.creekmoon.operationLog.hutool589.core.lang.intern.Interner;

/**
 * JDK中默认的字符串规范化实现
 *
 * @author looly
 * @since 5.4.3
 */
public class JdkStringInterner implements Interner<String> {
    @Override
    public String intern(String sample) {
        if (null == sample) {
            return null;
        }
        return sample.intern();
    }
}
