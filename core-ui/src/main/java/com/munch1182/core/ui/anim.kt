package com.munch1182.core.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavBackStackEntry
import com.ramcosta.composedestinations.animations.NavHostAnimatedDestinationStyle

/**
 * 防原生左滑的页面切换动画。
 *
 * 对应 XML 的 activity_open_enter/activity_close_exit 动画，
 * 组合函数需为页面大小，否则会错位。
 */
object PageAnimatedStyle : NavHostAnimatedDestinationStyle() {

    private const val TIME_FIRST = 400
    private const val TIME_SECOND = 300

    override val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = tween(TIME_FIRST, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(TIME_SECOND))
    }

    override val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutHorizontally(
            targetOffsetX = { -it / 3 },
            animationSpec = tween(TIME_FIRST, easing = FastOutSlowInEasing)
        ) + fadeOut(animationSpec = tween(TIME_SECOND))
    }

    override val popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideInHorizontally(
            initialOffsetX = { -it / 3 },
            animationSpec = tween(TIME_FIRST, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(TIME_SECOND))
    }

    override val popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = tween(TIME_FIRST, easing = FastOutSlowInEasing)
        ) + fadeOut(animationSpec = tween(TIME_SECOND))
    }
}
