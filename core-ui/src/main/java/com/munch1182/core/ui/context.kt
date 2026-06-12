package com.munch1182.core.ui

import androidx.fragment.app.FragmentActivity
import com.munch1182.lib.android.ActivityCurrHelper

val currOrThrow get() = ActivityCurrHelper.curr ?: error("use context but ActivityCurrHelper.curr is null")

val currAsFragmentActivityOrThrow get() = currOrThrow as? FragmentActivity ?: error("cannot use curr or as FragmentActivity")
