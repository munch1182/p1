package com.munch1182.p1.ui

import com.munch1182.p1.data.getTheme
import com.munch1182.p1.data.saveTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object ThemeHelper {
    private val _currThemeType = MutableStateFlow<ThemeType>(ThemeType.FollowSystem)
    private val _currDarkMode = MutableStateFlow(DarkMode.FollowSystem)

    init {
        getTheme().let { (type, mode) ->
            _currThemeType.value = type
            _currDarkMode.value = mode
        }
    }

    val currThemeType = _currThemeType.asStateFlow()
    val currDarkMode = _currDarkMode.asStateFlow()

    fun reset() = switch(ThemeType.FollowSystem, DarkMode.FollowSystem)

    /**
     * 更改主题和亮/暗模式
     *
     * 观察State进行实际的切换
     */
    fun switch(type: ThemeType = currThemeType.value, mode: DarkMode = currDarkMode.value) {
        _currThemeType.value = type
        _currDarkMode.value = mode
        saveTheme(type, mode)
    }
}

sealed class ThemeType {
    /**
     * 跟随系统： android12以下使用默认主题，以上使用动态主题
     */
    object FollowSystem : ThemeType()

    /**
     * 选中预设主题
     */
    data class Preset(val id: Long) : ThemeType()
}

enum class DarkMode {
    FollowSystem, Light, Dark
}