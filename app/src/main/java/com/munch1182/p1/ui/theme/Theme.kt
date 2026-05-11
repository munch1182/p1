package com.munch1182.p1.ui.theme

import android.annotation.SuppressLint
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.material.color.utilities.DynamicScheme
import com.google.android.material.color.utilities.Hct
import com.google.android.material.color.utilities.MaterialDynamicColors
import com.google.android.material.color.utilities.SchemeTonalSpot
import com.munch1182.p1.domain.DarkMode
import com.munch1182.p1.domain.ThemeType
import com.munch1182.p1.domain.ThemeVM

private val DarkColorScheme = darkColorScheme(
    primary = Purple80, secondary = PurpleGrey80, tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40, secondary = PurpleGrey40, tertiary = Pink40
)

@Composable
fun P1Theme(
    themeVM: ThemeVM = hiltViewModel(), content: @Composable () -> Unit
) {
    val curr by themeVM.currThemeData.collectAsStateWithLifecycle()

    val isDarkTheme = when (curr.second) {
        DarkMode.FollowSystem -> isSystemInDarkTheme()
        DarkMode.Light -> false
        DarkMode.Dark -> true
    }
    val colorScheme = when (val themeType = curr.first) {
        ThemeType.FollowSystem -> defaultColorScheme(isDarkTheme)
        is ThemeType.Preset -> selectColorScheme(themeType, isDarkTheme)
    }

    MaterialTheme(
        colorScheme = colorScheme, typography = Typography, content = content
    )
}

/**
 * 跟随系统：优先动态色彩（Android 12+），否则用默认亮/暗色
 */
@Composable
private fun defaultColorScheme(isDarkTheme: Boolean): ColorScheme {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        if (isDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (isDarkTheme) DarkColorScheme else LightColorScheme
    }
}

/**
 * 根据选中的颜色类型返回颜色
 */
private fun selectColorScheme(theme: ThemeType.Preset, isDarkTheme: Boolean): ColorScheme {
    val dyn = generateDynamicScheme(theme.id.toInt(), isDarkTheme)
    return dyn.toColorScheme(isDarkTheme)
}

/**
 * 源于[com.google.android.material:material]
 */
@SuppressLint("RestrictedApi")
private fun generateDynamicScheme(argb: Int, isDark: Boolean, contrastLevel: Double = 0.0): SchemeTonalSpot {
    return SchemeTonalSpot(Hct.fromInt(argb), isDark, contrastLevel)
}

@SuppressLint("RestrictedApi")
private fun DynamicScheme.toColorScheme(isDark: Boolean): ColorScheme {
    val dynamicColors = MaterialDynamicColors()

    val primary = Color(dynamicColors.primary().getArgb(this))
    val onPrimary = Color(dynamicColors.onPrimary().getArgb(this))
    val primaryContainer = Color(dynamicColors.primaryContainer().getArgb(this))
    val onPrimaryContainer = Color(dynamicColors.onPrimaryContainer().getArgb(this))
    val secondary = Color(dynamicColors.secondary().getArgb(this))
    val onSecondary = Color(dynamicColors.onSecondary().getArgb(this))
    val secondaryContainer = Color(dynamicColors.secondaryContainer().getArgb(this))
    val onSecondaryContainer = Color(dynamicColors.onSecondaryContainer().getArgb(this))
    val tertiary = Color(dynamicColors.tertiary().getArgb(this))
    val onTertiary = Color(dynamicColors.onTertiary().getArgb(this))
    val tertiaryContainer = Color(dynamicColors.tertiaryContainer().getArgb(this))
    val onTertiaryContainer = Color(dynamicColors.onTertiaryContainer().getArgb(this))
    val error = Color(dynamicColors.error().getArgb(this))
    val onError = Color(dynamicColors.onError().getArgb(this))
    val errorContainer = Color(dynamicColors.errorContainer().getArgb(this))
    val onErrorContainer = Color(dynamicColors.onErrorContainer().getArgb(this))
    val background = Color(dynamicColors.background().getArgb(this))
    val onBackground = Color(dynamicColors.onBackground().getArgb(this))
    val surface = Color(dynamicColors.surface().getArgb(this))
    val onSurface = Color(dynamicColors.onSurface().getArgb(this))
    val surfaceVariant = Color(dynamicColors.surfaceVariant().getArgb(this))
    val onSurfaceVariant = Color(dynamicColors.onSurfaceVariant().getArgb(this))
    val outline = Color(dynamicColors.outline().getArgb(this))
    val outlineVariant = Color(dynamicColors.outlineVariant().getArgb(this))
    val scrim = Color(dynamicColors.scrim().getArgb(this))
    val inverseSurface = Color(dynamicColors.inverseSurface().getArgb(this))
    val inverseOnSurface = Color(dynamicColors.inverseOnSurface().getArgb(this))
    val inversePrimary = Color(dynamicColors.inversePrimary().getArgb(this))
    val surfaceDim = Color(dynamicColors.surfaceDim().getArgb(this))
    val surfaceBright = Color(dynamicColors.surfaceBright().getArgb(this))
    val surfaceContainerLowest = Color(dynamicColors.surfaceContainerLowest().getArgb(this))
    val surfaceContainerLow = Color(dynamicColors.surfaceContainerLow().getArgb(this))
    val surfaceContainer = Color(dynamicColors.surfaceContainer().getArgb(this))
    val surfaceContainerHigh = Color(dynamicColors.surfaceContainerHigh().getArgb(this))
    val surfaceContainerHighest = Color(dynamicColors.surfaceContainerHighest().getArgb(this))

    return if (isDark) {
        darkColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            tertiary = tertiary,
            onTertiary = onTertiary,
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = onTertiaryContainer,
            error = error,
            onError = onError,
            errorContainer = errorContainer,
            onErrorContainer = onErrorContainer,
            background = background,
            onBackground = onBackground,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            outline = outline,
            outlineVariant = outlineVariant,
            scrim = scrim,
            inverseSurface = inverseSurface,
            inverseOnSurface = inverseOnSurface,
            inversePrimary = inversePrimary,
            surfaceDim = surfaceDim,
            surfaceBright = surfaceBright,
            surfaceContainerLowest = surfaceContainerLowest,
            surfaceContainerLow = surfaceContainerLow,
            surfaceContainer = surfaceContainer,
            surfaceContainerHigh = surfaceContainerHigh,
            surfaceContainerHighest = surfaceContainerHighest
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            tertiary = tertiary,
            onTertiary = onTertiary,
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = onTertiaryContainer,
            error = error,
            onError = onError,
            errorContainer = errorContainer,
            onErrorContainer = onErrorContainer,
            background = background,
            onBackground = onBackground,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            outline = outline,
            outlineVariant = outlineVariant,
            scrim = scrim,
            inverseSurface = inverseSurface,
            inverseOnSurface = inverseOnSurface,
            inversePrimary = inversePrimary,
            surfaceDim = surfaceDim,
            surfaceBright = surfaceBright,
            surfaceContainerLowest = surfaceContainerLowest,
            surfaceContainerLow = surfaceContainerLow,
            surfaceContainer = surfaceContainer,
            surfaceContainerHigh = surfaceContainerHigh,
            surfaceContainerHighest = surfaceContainerHighest
        )
    }
}