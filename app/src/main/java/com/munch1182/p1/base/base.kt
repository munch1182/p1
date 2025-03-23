package com.munch1182.p1.base

import android.app.Activity
import androidx.fragment.app.FragmentActivity
import com.munch1182.lib.helper.ActivityCurrHelper

val curr: Activity
    get() = ActivityCurrHelper.curr!!

val currFM: FragmentActivity
    get() = curr as FragmentActivity