package com.munch1182.p1.base

import com.munch1182.core.common.INotice

object Notice : INotice {
    override fun toast(message: String) {
    }

    override suspend fun awaitToast(message: String) {
    }

    override suspend fun alertYesNo(message: String, title: String?): Boolean {
        return false
    }

}