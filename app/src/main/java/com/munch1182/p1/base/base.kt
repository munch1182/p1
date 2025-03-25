package com.munch1182.p1.base

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentActivity
import com.munch1182.lib.helper.ActivityCurrHelper

val curr: Activity
    get() = ActivityCurrHelper.curr!!

val currFM: FragmentActivity
    get() = curr as FragmentActivity

@Composable
fun str(id: Int) = stringResource(id)