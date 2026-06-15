package com.munch1182.core.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.munch1182.core.ui.stateInWithStarted
import com.munch1182.lib.android.DataHelper
import com.munch1182.lib.common.AnalyticsTracker
import com.munch1182.lib.common.get
import com.munch1182.lib.common.launchIO
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * 主题类型。
 */
sealed class ThemeType {
    /** 跟随系统动态颜色与深色模式。 */
    object FollowSystem : ThemeType()

    /** 手动选择的预设主题颜色。 */
    data class Preset(val id: Long) : ThemeType()
}

/**
 * 深色模式选项。
 */
enum class DarkMode {
    FollowSystem, Light, Dark
}

/**
 * 主题数据：[ThemeType] 与 [DarkMode] 的组合。
 */
typealias ThemeData = Pair<ThemeType, DarkMode>

/**
 * 主题数据仓库接口。
 */
interface ThemeRepo {
    /** 获取当前主题数据 Flow。 */
    fun getThemeData(): Flow<ThemeData>

    /** 持久化保存主题设置。 */
    suspend fun setThemeData(type: ThemeType, mode: DarkMode)
}

/**
 * 主题 ViewModel，管理主题切换与持久化。
 */
@HiltViewModel
class ThemeVM @Inject constructor(
    private val repo: ThemeRepo,
    private val analytics: AnalyticsTracker
) : ViewModel() {
    /** 当前主题数据。 */
    val currThemeData = repo.getThemeData().stateInWithStarted(
        viewModelScope, ThemeType.FollowSystem to DarkMode.FollowSystem
    )

    /**
     * 切换主题，可单独指定 [type] 或 [mode]，未指定时保持当前值。
     */
    fun switch(type: ThemeType = currThemeData.value.first, mode: DarkMode = currThemeData.value.second) {
        viewModelScope.launchIO {
            analytics.trackEvent("切换主题", mapOf("主题" to type, "模式" to mode))
            repo.setThemeData(type, mode)
        }
    }

    /** 重置为跟随系统。 */
    fun reset() = switch(ThemeType.FollowSystem, DarkMode.FollowSystem)
}

/**
 * 基于 DataHelper(MMKV) 的主题数据仓库实现。
 */
class ThemeRepoImpl @Inject constructor() : ThemeRepo {

    override fun getThemeData(): Flow<ThemeData> {
        val flowThemeType = DataHelper.get(KEY_SAVE_THEME, 0L).map(::long2ThemeType)
        val flowMode = DataHelper.get(KEY_SAVE_LIGHT_MODE, 0).map(::long2Mode)
        return flowThemeType.combine(flowMode) { theme, mode -> theme to mode }
    }

    override suspend fun setThemeData(type: ThemeType, mode: DarkMode) {
        DataHelper.put(KEY_SAVE_THEME, type.themeType2Long())
        DataHelper.put(KEY_SAVE_LIGHT_MODE, mode.mode2Long())
    }

    private fun DarkMode.mode2Long() = when (this) {
        DarkMode.FollowSystem -> 0
        DarkMode.Light -> 1
        DarkMode.Dark -> 2
    }

    private fun long2Mode(value: Int) = when (value) {
        1 -> DarkMode.Light
        2 -> DarkMode.Dark
        else -> DarkMode.FollowSystem
    }

    private fun ThemeType.themeType2Long() = when (this) {
        is ThemeType.FollowSystem -> 0L
        is ThemeType.Preset -> id
    }

    private fun long2ThemeType(value: Long) = when (value) {
        0L -> ThemeType.FollowSystem
        else -> ThemeType.Preset(value)
    }
}
