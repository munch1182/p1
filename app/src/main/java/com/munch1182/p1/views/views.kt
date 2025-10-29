package com.munch1182.p1.views

import android.content.Intent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.munch1182.lib.helper.ActivityCurrHelper
import com.munch1182.p1.ui.ClickButton
import com.munch1182.p1.ui.RvPage
import com.munch1182.p1.views.other.NetPhoneActivity

@kotlinx.serialization.Serializable
sealed class Screen(val name: String) {
    @kotlinx.serialization.Serializable
    object Home : Screen("首页")

    @kotlinx.serialization.Serializable
    object About : Screen("关于")

    @kotlinx.serialization.Serializable
    object Result : Screen("权限相关")

    @kotlinx.serialization.Serializable
    object Scan : Screen("扫码相关"), IntentScreen {
        override val intent: Intent get() = Intent(currNow, ScanActivity::class.java)
    }

    @kotlinx.serialization.Serializable
    object Audio : Screen("音频相关")

    @kotlinx.serialization.Serializable
    object Bluetooth : Screen("蓝牙相关")

    @kotlinx.serialization.Serializable
    object Other : Screen("其它")

    @kotlinx.serialization.Serializable
    object NetPhone : Screen("视频通话"), IntentScreen {
        override val intent: Intent get() = Intent(currNow, NetPhoneActivity::class.java)
    }
}

private interface IntentScreen {
    val intent: Intent
}

typealias NavGraph = @Composable AnimatedContentScope.(NavHostController) -> Unit

val mainScreens: Array<Pair<Screen, NavGraph>> by lazy {
    arrayOf(
        Screen.Home to { nav -> ItemView(ignoreFirst = true) { nav.goTo(it) } },
        Screen.Result to { currNow.ResultView() },
        Screen.Scan to { },
        Screen.Audio to { AudioView() },
        Screen.Bluetooth to { BluetoothView() },
        Screen.Other to { nav -> ItemView(otherScreens) { nav.goTo(it) } },
        Screen.About to { AboutView() },
    )
}

val otherScreens: Array<Pair<Screen, NavGraph>> by lazy {
    arrayOf(Screen.NetPhone to {})
}

private fun NavHostController.goTo(it: Screen) {
    if (it is IntentScreen) currNow.startActivity(it.intent) else navigate(it.name)
}

@Composable
fun AppView(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Screen.Home.name, modifier = modifier) {
        mainScreens.forEach { s -> // 保持顺序一致
            composableInAnim(s.first) { s.second.invoke(this, navController) }
        }
    }
}

val curr get() = ActivityCurrHelper.curr as? FragmentActivity
val currNow get() = curr ?: throw NullPointerException("curr is null")
private typealias ContentView = @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit

private fun NavGraphBuilder.composableInAnim(
    route: Screen, enter: Enter? = slideInFromRight(), exit: Exit? = slideOutToLeft(), popEnter: Enter? = slideInFromLeft(), popExit: Exit? = slideOutToRight(), content: ContentView
) {
    composable(
        route.name, content = content, enterTransition = enter, exitTransition = exit, popEnterTransition = popEnter, popExitTransition = popExit
    )
}

private const val animTime = 350
private typealias Enter = AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition?
private typealias Exit = AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition?

/** 从左侧滑入（常用于前进到新页面） */
private fun slideInFromLeft(time: Int = animTime, easing: Easing = FastOutSlowInEasing): Enter = {
    slideInHorizontally(
        initialOffsetX = { -it }, animationSpec = tween(durationMillis = time, easing = easing)
    )
}

/** 向左侧滑出（常用于离开当前页面） */
private fun slideOutToLeft(time: Int = animTime, easing: Easing = FastOutSlowInEasing): Exit = {
    slideOutHorizontally(
        targetOffsetX = { -it }, animationSpec = tween(durationMillis = time, easing = easing)
    )
}

/** 从右侧滑入（常用于返回上一页） */
private fun slideInFromRight(time: Int = animTime, easing: Easing = FastOutSlowInEasing): Enter = {
    slideInHorizontally(
        initialOffsetX = { it }, animationSpec = tween(durationMillis = time, easing = easing)
    )
}

/** 向右侧滑出（常用于返回时的页面退出） */
private fun slideOutToRight(time: Int = animTime, easing: Easing = FastOutSlowInEasing): Exit = {
    slideOutHorizontally(
        targetOffsetX = { it }, animationSpec = tween(durationMillis = time, easing = easing)
    )
}

@Composable
fun ItemView(items: Array<Pair<Screen, NavGraph>> = mainScreens, ignoreFirst: Boolean = false, goTo: ((Screen) -> Unit)? = null) {
    RvPage(items.mapIndexedNotNull { i, it -> if (ignoreFirst && i == 0) null else it }.toTypedArray()) {
        ClickButton(it.first.name) { goTo?.invoke(it.first) }
    }
}
