package com.tencent.mars.xlog;

/**
 * Mars Xlog 的 JNI 桥接类，提供高性能日志写入能力（压缩、加密、异步写入）。
 * 仅保留必要的 native 方法声明，使用时需确保对应的 native 库已加载。
 * <p>
 * 简化方法 {@link #log(int, String, String)}
 * 适合大多数日常使用场景。
 *
 * @see <a href="https://github.com/Tencent/mars#mars_cn">mars</a>
 */
public class Xlog {

    // ------------------------------ 日志级别常量 ------------------------------
    public static final int LEVEL_VERBOSE = 0;
    public static final int LEVEL_DEBUG = 1;
    public static final int LEVEL_INFO = 2;
    public static final int LEVEL_WARNING = 3;
    public static final int LEVEL_ERROR = 4;
    public static final int LEVEL_FATAL = 5;
    /**
     * 不输出任何日志
     */
    public static final int LEVEL_NONE = 6;

    // ------------------------------ 写入模式 ------------------------------
    /**
     * 异步写入模式（推荐，高性能）
     */
    public static final int AppenderModeAsync = 0;
    /**
     * 同步写入模式（会阻塞调用线程，用于关键日志）
     */
    public static final int AppenderModeSync = 1;

    // ------------------------------ 压缩算法 ------------------------------
    /**
     * ZLIB 压缩（默认，兼容性好）
     */
    public static final int ZLIB_MODE = 0;
    /**
     * ZSTD 压缩（压缩率更高，速度更快）
     */
    public static final int ZSTD_MODE = 1;

    /**
     * Xlog 配置类，用于初始化日志实例。
     */
    public static class XLogConfig {
        /**
         * 日志输出级别，低于此级别的日志将被忽略，默认为 {@link #LEVEL_INFO}
         */
        public int level = LEVEL_INFO;
        /**
         * 写入模式，默认为 {@link #AppenderModeAsync}
         */
        public int mode = AppenderModeAsync;
        /**
         * 日志文件存储目录（必须存在且有写入权限）
         */
        public String logdir;
        /**
         * 日志文件名前缀，实际文件名为 nameprefix_YYYYMMDD.xlog
         */
        public String nameprefix;
        /**
         * RSA 公钥，用于日志加密，为空时不加密
         */
        public String pubkey = "";
        /**
         * 压缩算法，默认为 {@link #ZLIB_MODE}
         */
        public int compressmode = ZLIB_MODE;
        /**
         * 压缩级别 0-9，0 表示使用默认级别
         */
        public int compresslevel = 0;
        /**
         * 缓存目录（用于存放未及时写入的缓存文件）
         */
        public String cachedir;
        /**
         * 缓存文件保留天数，超过此天数的缓存文件会被清理
         */
        public int cachedays = 0;
    }

    // ------------------------------ native 核心方法 ------------------------------

    /**
     * 写入一条日志到指定的 Xlog 实例。
     * 该方法为 native 实现，会进行格式化、压缩、加密并最终落盘。
     *
     * @param instancePtr 使用#{@link #appenderOpen}创建的实例指针, 为0
     * @param level       日志级别（LEVEL_* 常量）
     * @param tag         日志标签
     * @param filename    源文件名（可传空字符串）
     * @param funcname    函数名（可传空字符串）
     * @param line        行号（可传 0）
     * @param pid         进程 ID
     * @param tid         线程 ID
     * @param maintid     主线程 ID
     * @param log         日志内容
     */
    public static native void logWrite2(long instancePtr, int level, String tag, String filename, String funcname, int line, int pid, long tid, long maintid, String log);

    /**
     * 创建一个实例, 并作为默认的写入实例, 该实例的instancePtr为0
     */
    public static native void appenderOpen(XLogConfig logConfig);

    /**
     * 关闭当前默认实例，释放相关资源。
     */
    public native void appenderClose();

    // ------------------------------ 简化方法 ------------------------------

    /**
     * 简化日志写入方法，文件名、函数名、行号、进程/线程信息都设置为空。
     * <p>
     *
     * @param level   日志级别（LEVEL_* 常量）
     * @param tag     日志标签
     * @param message 日志内容
     */
    public static void log(int level, String tag, String message) {
        // 调用原生写入方法，文件名、函数名、行号使用默认值
        logWrite2(0, level, tag, "", "", -1, -1, -1, -1, message);
    }
}