package com.munch1182.p1.domain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.munch1182.core.android.DataHelper
import com.munch1182.core.common.get
import com.munch1182.core.common.launchIO
import com.munch1182.p1.base.AppAnalytics
import com.munch1182.p1.base.stateInWithStarted
import com.munch1182.p1.data.KEY_SAVE_LIGHT_MODE
import com.munch1182.p1.data.KEY_SAVE_THEME
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * 主题类型
 */
sealed class ThemeType {
    /**
     * 跟随系统: 使用动态颜色/默认主题(不支持动态颜色的)+系统深色模式状态
     */
    object FollowSystem : ThemeType()

    /**
     * 手动选择一种预制主题
     */
    data class Preset(val id: Long) : ThemeType()
}

/**
 * 深色模式: 跟随系统/浅色/深色
 */
enum class DarkMode {
    FollowSystem, Light, Dark
}

/**
 * 主题数据类型
 */
typealias ThemeData = Pair<ThemeType, DarkMode>

/**
 * 主题数据仓库
 */
interface ThemeRepo {
    /**
     * 获取当前主题数据
     *
     * 注意: 这里返回的是Flow, 主题更改时流会返回(除非被重启)
     */
    fun getThemeData(): Flow<ThemeData>

    /**
     * 设置主题数据
     */
    suspend fun setThemeData(type: ThemeType, mode: DarkMode)
}

/**
 * 主题数据VM
 */
@HiltViewModel
class ThemeVM @Inject constructor(private val repo: ThemeRepo) : ViewModel() {
    /**
     * 获取当前主题数据
     * 注意: 这里返回的是Flow, 主题更改时流会返回(除非被重启)
     */
    val currThemeData = repo.getThemeData().stateInWithStarted(
        viewModelScope, ThemeType.FollowSystem to DarkMode.FollowSystem
    )

    /**
     * 切换主题
     */
    fun switch(type: ThemeType = currThemeData.value.first, mode: DarkMode = currThemeData.value.second) {
        viewModelScope.launchIO {
            AppAnalytics.trackEvent("切换主题", mapOf("主题" to type, "模式" to mode))
            repo.setThemeData(type, mode)
        }
    }

    /**
     * 重置为默认主题
     */
    fun reset() = switch(ThemeType.FollowSystem, DarkMode.FollowSystem)
}

/**
 * 主题数据仓库实现
 */
class ThemeRepoImpl @Inject constructor() : ThemeRepo {

    override fun getThemeData(): Flow<ThemeData> {
        val flowThemeType = DataHelper.get(KEY_SAVE_THEME, 0L).map(::long2ThemeType)
        val flowMode = DataHelper.get(KEY_SAVE_LIGHT_MODE, 0).map(::long2Mode)
        return flowThemeType.combine(flowMode) { theme, mode -> theme to mode }
    }

    override suspend fun setThemeData(type: ThemeType, mode: DarkMode) {
        val themeTypeLong = type.themeType2Long()
        DataHelper.put(KEY_SAVE_THEME, themeTypeLong)
        val modeLong = mode.mode2Long()
        DataHelper.put(KEY_SAVE_LIGHT_MODE, modeLong)
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