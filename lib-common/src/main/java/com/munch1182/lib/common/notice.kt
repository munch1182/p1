package com.munch1182.lib.common

/**
 * 通知能力接口。
 *
 * 用于表达语义, 并预留更改样式的能力.
 *
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
}