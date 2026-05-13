package com.munch1182.core.common

/**
 * 通知能力接口。
 *
 * 提供跨平台的基础通知功能，包括：
 * - 短暂提示（Toast 风格）
 * - 可等待结束的短暂提示（用于顺序展示）
 * - 简单的是/否确认弹窗
 *
 * 其余更复杂内容/交互的通知应该使用自定义的Dialog或者其它实现
 */
interface INotice {
    /**
     * 显示短暂提示（如 Toast），自动消失，不阻塞调用方。
     *
     * 该方法为普通函数，调用后立即返回，无需等待提示消失。
     *
     * @param message 提示文本内容
     */
    fun toast(message: String)

    /**
     * 显示短暂提示(时间由平台默认设置)，并挂起直到提示消失后恢复。
     *
     * 适用于需要等待前一个提示结束再执行后续逻辑的场景（例如顺序播放多个提示）。
     *
     * @param message 提示文本内容
     */
    suspend fun awaitToast(message: String)

    /**
     * 显示一个包含“是”/“否”两个按钮的确认弹窗，挂起等待用户选择。
     *
     * @param message 弹窗内容文本
     * @param title 可选标题，默认为 null（部分平台可能忽略或显示默认标题）
     * @return `true` 表示用户点击“是”，`false` 表示用户点击“否”或取消关闭弹窗
     */
    suspend fun alertYesNo(message: String, title: String? = null): Boolean
}