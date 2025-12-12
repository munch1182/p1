package com.munch1182.p1.base

import com.munch1182.android.lib.helper.ActivityCurrHelper
import com.munch1182.android.net.newHttpClientBuilder

val ctx get() = ActivityCurrHelper.curr

val okhttp by lazy { newHttpClientBuilder().build() }