package cn.creekmoon.operationLog.hutool589.core.swing.clipboard;

import cn.creekmoon.operationLog.hutool589.core.exceptions.UtilException;
import cn.creekmoon.operationLog.hutool589.core.swing.clipboard.ClipboardListener;
import cn.creekmoon.operationLog.hutool589.core.swing.clipboard.ClipboardMonitor;
import cn.creekmoon.operationLog.hutool589.core.swing.clipboard.ImageSelection;

import java.awt.*;
import java.awt.datatransfer.*;
import java.io.IOException;

/**
 * 系统剪贴板工具类
 *
 * @author looly
 * @since 3.2.0
 */
public class ClipboardUtil {

    /**
     * 获取系统剪贴板
     *
     * @return {@link Clipboard}
     */
    public static Clipboard getClipboard() {
        return Toolkit.getDefaultToolkit().getSystemClipboard();
    }

    /**
     * 设置内容到剪贴板
     *
     * @param contents 内容
     */
    public static void set(Transferable contents) {
        set(contents, null);
    }

    /**
     * 设置内容到剪贴板
     *
     * @param contents 内容
     * @param owner    所有者
     */
    public static void set(Transferable contents, ClipboardOwner owner) {
        getClipboard().setContents(contents, owner);
    }

    /**
     * 获取剪贴板内容
     *
     * @param flavor 数据元信息，标识数据类型
     * @return 剪贴板内容，类型根据flavor不同而不同
     */
    public static Object get(DataFlavor flavor) {
        return get(getClipboard().getContents(null), flavor);
    }

    /**
     * 获取剪贴板内容
     *
     * @param content {@link Transferable}
     * @param flavor  数据元信息，标识数据类型
     * @return 剪贴板内容，类型根据flavor不同而不同
     */
    public static Object get(Transferable content, DataFlavor flavor) {
        if (null != content && content.isDataFlavorSupported(flavor)) {
            try {
                return content.getTransferData(flavor);
            } catch (UnsupportedFlavorException | IOException e) {
                throw new UtilException(e);
            }
        }
        return null;
    }

    /**
     * 设置字符串文本到剪贴板
     *
     * @param text 字符串文本
     */
    public static void setStr(String text) {
        set(new StringSelection(text));
    }

    /**
     * 从剪贴板获取文本
     *
     * @return 文本
     */
    public static String getStr() {
        return (String) get(DataFlavor.stringFlavor);
    }

    /**
     * 从剪贴板的{@link Transferable}获取文本
     *
     * @param content {@link Transferable}
     * @return 文本
     * @since 4.5.6
     */
    public static String getStr(Transferable content) {
        return (String) get(content, DataFlavor.stringFlavor);
    }

    /**
     * 设置图片到剪贴板
     *
     * @param image 图像
     */
    public static void setImage(Image image) {
        set(new ImageSelection(image), null);
    }

    /**
     * 从剪贴板获取图片
     *
     * @return 图片{@link Image}
     */
    public static Image getImage() {
        return (Image) get(DataFlavor.imageFlavor);
    }

    /**
     * 从剪贴板的{@link Transferable}获取图片
     *
     * @param content {@link Transferable}
     * @return 图片
     * @since 4.5.6
     */
    public static Image getImage(Transferable content) {
        return (Image) get(content, DataFlavor.imageFlavor);
    }

    /**
     * 监听剪贴板修改事件
     *
     * @param listener 监听处理接口
     * @see cn.creekmoon.operationLog.hutool589.core.swing.clipboard.ClipboardMonitor#listen(boolean)
     * @since 4.5.6
     */
    public static void listen(cn.creekmoon.operationLog.hutool589.core.swing.clipboard.ClipboardListener listener) {
        listen(listener, true);
    }

    /**
     * 监听剪贴板修改事件
     *
     * @param listener 监听处理接口
     * @param sync     是否同步阻塞
     * @see cn.creekmoon.operationLog.hutool589.core.swing.clipboard.ClipboardMonitor#listen(boolean)
     * @since 4.5.6
     */
    public static void listen(cn.creekmoon.operationLog.hutool589.core.swing.clipboard.ClipboardListener listener, boolean sync) {
        listen(cn.creekmoon.operationLog.hutool589.core.swing.clipboard.ClipboardMonitor.DEFAULT_TRY_COUNT, cn.creekmoon.operationLog.hutool589.core.swing.clipboard.ClipboardMonitor.DEFAULT_DELAY, listener, sync);
    }

    /**
     * 监听剪贴板修改事件
     *
     * @param tryCount 尝试获取剪贴板内容的次数
     * @param delay    响应延迟，当从第二次开始，延迟一定毫秒数等待剪贴板可以获取
     * @param listener 监听处理接口
     * @param sync     是否同步阻塞
     * @see cn.creekmoon.operationLog.hutool589.core.swing.clipboard.ClipboardMonitor#listen(boolean)
     * @since 4.5.6
     */
    public static void listen(int tryCount, long delay, ClipboardListener listener, boolean sync) {
        ClipboardMonitor.INSTANCE//
                .setTryCount(tryCount)//
                .setDelay(delay)//
                .addListener(listener)//
                .listen(sync);
    }
}
