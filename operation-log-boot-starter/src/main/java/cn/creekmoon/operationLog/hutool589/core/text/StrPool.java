package cn.creekmoon.operationLog.hutool589.core.text;

import cn.creekmoon.operationLog.hutool589.core.text.CharPool;
import cn.creekmoon.operationLog.hutool589.core.util.XmlUtil;

/**
 * 常用字符串常量定义
 *
 * @author looly
 * @see cn.creekmoon.operationLog.hutool589.core.text.CharPool
 * @since 5.6.3
 */
public interface StrPool {

    /**
     * 字符常量：空格符 {@code ' '}
     */
    char C_SPACE = cn.creekmoon.operationLog.hutool589.core.text.CharPool.SPACE;

    /**
     * 字符常量：制表符 {@code '\t'}
     */
    char C_TAB = cn.creekmoon.operationLog.hutool589.core.text.CharPool.TAB;

    /**
     * 字符常量：点 {@code '.'}
     */
    char C_DOT = cn.creekmoon.operationLog.hutool589.core.text.CharPool.DOT;

    /**
     * 字符常量：斜杠 {@code '/'}
     */
    char C_SLASH = cn.creekmoon.operationLog.hutool589.core.text.CharPool.SLASH;

    /**
     * 字符常量：反斜杠 {@code '\\'}
     */
    char C_BACKSLASH = cn.creekmoon.operationLog.hutool589.core.text.CharPool.BACKSLASH;

    /**
     * 字符常量：回车符 {@code '\r'}
     */
    char C_CR = cn.creekmoon.operationLog.hutool589.core.text.CharPool.CR;

    /**
     * 字符常量：换行符 {@code '\n'}
     */
    char C_LF = cn.creekmoon.operationLog.hutool589.core.text.CharPool.LF;

    /**
     * 字符常量：下划线 {@code '_'}
     */
    char C_UNDERLINE = cn.creekmoon.operationLog.hutool589.core.text.CharPool.UNDERLINE;

    /**
     * 字符常量：逗号 {@code ','}
     */
    char C_COMMA = cn.creekmoon.operationLog.hutool589.core.text.CharPool.COMMA;

    /**
     * 字符常量：花括号（左） <code>'{'</code>
     */
    char C_DELIM_START = cn.creekmoon.operationLog.hutool589.core.text.CharPool.DELIM_START;

    /**
     * 字符常量：花括号（右） <code>'}'</code>
     */
    char C_DELIM_END = cn.creekmoon.operationLog.hutool589.core.text.CharPool.DELIM_END;

    /**
     * 字符常量：中括号（左） {@code '['}
     */
    char C_BRACKET_START = cn.creekmoon.operationLog.hutool589.core.text.CharPool.BRACKET_START;

    /**
     * 字符常量：中括号（右） {@code ']'}
     */
    char C_BRACKET_END = cn.creekmoon.operationLog.hutool589.core.text.CharPool.BRACKET_END;

    /**
     * 字符常量：冒号 {@code ':'}
     */
    char C_COLON = cn.creekmoon.operationLog.hutool589.core.text.CharPool.COLON;

    /**
     * 字符常量：艾特 {@code '@'}
     */
    char C_AT = CharPool.AT;

    /**
     * 字符串常量：制表符 {@code "\t"}
     */
    String TAB = "	";

    /**
     * 字符串常量：点 {@code "."}
     */
    String DOT = ".";

    /**
     * 字符串常量：双点 {@code ".."} <br>
     * 用途：作为指向上级文件夹的路径，如：{@code "../path"}
     */
    String DOUBLE_DOT = "..";

    /**
     * 字符串常量：斜杠 {@code "/"}
     */
    String SLASH = "/";

    /**
     * 字符串常量：反斜杠 {@code "\\"}
     */
    String BACKSLASH = "\\";

    /**
     * 字符串常量：回车符 {@code "\r"} <br>
     * 解释：该字符常用于表示 Linux 系统和 MacOS 系统下的文本换行
     */
    String CR = "\r";

    /**
     * 字符串常量：换行符 {@code "\n"}
     */
    String LF = "\n";

    /**
     * 字符串常量：Windows 换行 {@code "\r\n"} <br>
     * 解释：该字符串常用于表示 Windows 系统下的文本换行
     */
    String CRLF = "\r\n";

    /**
     * 字符串常量：下划线 {@code "_"}
     */
    String UNDERLINE = "_";

    /**
     * 字符串常量：减号（连接符） {@code "-"}
     */
    String DASHED = "-";

    /**
     * 字符串常量：逗号 {@code ","}
     */
    String COMMA = ",";

    /**
     * 字符串常量：花括号（左） <code>"{"</code>
     */
    String DELIM_START = "{";

    /**
     * 字符串常量：花括号（右） <code>"}"</code>
     */
    String DELIM_END = "}";

    /**
     * 字符串常量：中括号（左） {@code "["}
     */
    String BRACKET_START = "[";

    /**
     * 字符串常量：中括号（右） {@code "]"}
     */
    String BRACKET_END = "]";

    /**
     * 字符串常量：冒号 {@code ":"}
     */
    String COLON = ":";

    /**
     * 字符串常量：艾特 {@code "@"}
     */
    String AT = "@";


    /**
     * 字符串常量：HTML 空格转义 {@code "&nbsp;" -> " "}
     */
    String HTML_NBSP = XmlUtil.NBSP;

    /**
     * 字符串常量：HTML And 符转义 {@code "&amp;" -> "&"}
     */
    String HTML_AMP = XmlUtil.AMP;

    /**
     * 字符串常量：HTML 双引号转义 {@code "&quot;" -> "\""}
     */
    String HTML_QUOTE = XmlUtil.QUOTE;

    /**
     * 字符串常量：HTML 单引号转义 {@code "&apos" -> "'"}
     */
    String HTML_APOS = XmlUtil.APOS;

    /**
     * 字符串常量：HTML 小于号转义 {@code "&lt;" -> "<"}
     */
    String HTML_LT = XmlUtil.LT;

    /**
     * 字符串常量：HTML 大于号转义 {@code "&gt;" -> ">"}
     */
    String HTML_GT = XmlUtil.GT;

    /**
     * 字符串常量：空 JSON {@code "{}"}
     */
    String EMPTY_JSON = "{}";
}
