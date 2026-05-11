package com.munch1182.p1.ui

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
 * 防原左滑的页面切换动画
 *
 * 注意: 如果在compose中使用, 要保证compose函数是页面大小的, 否则会错位
 */
object PageAnimatedStyle : NavHostAnimatedDestinationStyle() {

    private const val TIME_FIRST = 400
    private const val TIME_SECOND = 300

    // 对应 XML: activity_open_enter (新页面进入)
    override val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideInHorizontally(
            initialOffsetX = { it }, // 从右侧 100% 宽度处开始
            animationSpec = tween(TIME_FIRST, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(TIME_SECOND))
    }

    // 对应 XML: activity_open_exit (旧页面被覆盖)
    override val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutHorizontally(
            targetOffsetX = { -it / 3 }, // 旧页面只向左移动 1/3，模拟“被压在下面”的层次感
            animationSpec = tween(TIME_FIRST, easing = FastOutSlowInEasing)
        ) + fadeOut(animationSpec = tween(TIME_SECOND))
    }

    // 对应 XML: activity_close_enter (返回时，旧页面重新显示)
    override val popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideInHorizontally(
            initialOffsetX = { -it / 3 }, // 从左侧 1/3 处滑回
            animationSpec = tween(TIME_FIRST, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(TIME_SECOND))
    }

    // 对应 XML: activity_close_exit (返回时，当前页面退出)
    override val popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutHorizontally(
            targetOffsetX = { it }, // 向右侧 100% 宽度划出
            animationSpec = tween(TIME_FIRST, easing = FastOutSlowInEasing)
        ) + fadeOut(animationSpec = tween(TIME_SECOND))
    }
}

