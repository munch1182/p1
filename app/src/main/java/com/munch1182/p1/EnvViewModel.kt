package com.munch1182.p1

import androidx.lifecycle.ViewModel
import com.munch1182.lib.common.AnalyticsTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject

/**
 * compose不能注入参数, 使用此vm来为compose提供常用环境参数
 */
@HiltViewModel
class EnvViewModel @Inject constructor(val analytics: AnalyticsTracker) : ViewModel()