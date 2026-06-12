package com.munch1182.lib.android

import android.view.View

/**
 * 单个数据更新回调
 */
fun interface OnUpdateListener<T> {

    /**
     * 数据更新
     */
    fun onUpdate(data: T)
}

/**
 * 提供一个view
 */
fun interface OnViewProvider<T> {
    fun provideView(ctx: T): View
}