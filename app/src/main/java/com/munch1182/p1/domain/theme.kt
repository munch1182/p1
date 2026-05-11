package com.munch1182.p1.domain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.munch1182.core.android.DataHelper
import com.munch1182.core.common.get
import com.munch1182.core.common.launchIO
import com.munch1182.p1.base.AppAnalytics
import com.munch1182.p1.base.stateIn
import com.munch1182.p1.data.KEY_SAVE_LIGHT_MODE
import com.munch1182.p1.data.KEY_SAVE_THEME
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

sealed class ThemeType {
    object FollowSystem : ThemeType()
    data class Preset(val id: Long) : ThemeType()
}

enum class DarkMode {
    FollowSystem, Light, Dark
}

typealias ThemeData = Pair<ThemeType, DarkMode>

interface ThemeRepo {
    fun getThemeData(): Flow<ThemeData>
    suspend fun setThemeData(type: ThemeType, mode: DarkMode)
}

@HiltViewModel
class ThemeVM @Inject constructor(private val repo: ThemeRepo) : ViewModel() {
    val currThemeData = repo.getThemeData().stateIn(
        viewModelScope, ThemeType.FollowSystem to DarkMode.FollowSystem
    )

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