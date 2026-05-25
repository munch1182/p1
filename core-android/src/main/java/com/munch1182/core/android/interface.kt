package com.munch1182.core.android

/**
 * 单个数据更新回调
 */
fun interface OnUpdateListener<T> {

    /**
     * 数据更新
     */
    fun onUpdate(data: T)
}