package com.munch1182.lib.android.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.content.ComponentName
import android.content.Context
import android.graphics.Path
import android.graphics.Rect
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityNodeInfo
import com.munch1182.lib.android.AppHelper
import kotlinx.coroutines.delay

/**
 * 判断无障碍服务是否已经开启
 */
fun isAccessibilityEnabled(
    clazz: Class<out AccessibilityService>,
    feedback: Int = AccessibilityServiceInfo.FEEDBACK_ALL_MASK,
    ctx: Context = AppHelper,
): Boolean {
    val manager = ctx.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    val list = manager.getEnabledAccessibilityServiceList(feedback)
    val targetComponent = ComponentName(ctx, clazz).flattenToShortString()
    return list.any { it.id == targetComponent }
}

/**
 * 点击该 node；如果 node 可点击则直接调用点击，否则使用手势模拟点击。
 * 注意：如果有动画，调用者需要自行预留动画时间。
 * 需要在服务配置中设置 android:canPerformGestures="true"。
 *
 * @param retryTimes 重试次数（含首次），每次失败后延迟 (100ms * 当前次数) 再试
 * @return 是否已经执行过点击（不保证点击完成，手势是异步的）
 */
suspend fun AccessibilityNodeInfo.startClickIfCan(
    retryTimes: Int = 2, server: AccessibilityService = AccessibilityServiceHelper.server ?: throw IllegalStateException("AccessibilityService not started")
): Boolean {
    var currentRetry = 0
    while (currentRetry < retryTimes) {
        val node = this
        if (!node.isVisibleToUser) return false

        // 优先尝试 ACTION_CLICK
        if (node.isClickable && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
            return true
        }

        // 手势模拟点击
        val rect = Rect()
        node.getBoundsInScreen(rect)
        if (!rect.isEmpty) {
            val path = Path().apply {
                val x = rect.centerX().toFloat()
                val y = rect.centerY().toFloat()
                moveTo(x, y)
            }
            val stroke = GestureDescription.StrokeDescription(path, 0, 1)
            val gesture = GestureDescription.Builder().addStroke(stroke).build()
            server.dispatchGesture(gesture, null, null)
            return true
        }

        // 重试前等待
        currentRetry++
        if (currentRetry < retryTimes) {
            delay(100L * currentRetry)
        }
    }
    return false
}


/**
 * 挂起版本的点击（推荐）
 */
suspend fun AccessibilityNodeInfo.startClickIfCanSuspend(
    retryTimes: Int = 1, server: AccessibilityService = AccessibilityServiceHelper.server ?: throw IllegalStateException("AccessibilityService not started")
): Boolean {
    repeat(retryTimes) { attempt ->
        if (!isVisibleToUser) return false
        if (isClickable && performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
        val rect = Rect().also(::getBoundsInScreen)
        if (!rect.isEmpty) {
            val path = Path().apply {
                val x = rect.centerX().toFloat()
                val y = rect.centerY().toFloat()
                moveTo(x, y)
            }
            val stroke = GestureDescription.StrokeDescription(path, 0, 1)
            val gesture = GestureDescription.Builder().addStroke(stroke).build()
            server.dispatchGesture(gesture, null, null)
            return true
        }
        if (attempt < retryTimes - 1) delay(100L * (attempt + 1))
    }
    return false
}

/**
 * 从这个节点中向下寻找到第一个允许滑动的节点。
 * 注意：复杂页面可能有多个可滑动元素，调用者需自行验证。
 * 返回的节点由调用者负责回收。
 */
fun AccessibilityNodeInfo.findScrollRoot(): AccessibilityNodeInfo? {
    if (isScrollable) return this
    for (i in 0 until childCount) {
        val child = getChild(i) ?: continue
        val found = child.findScrollRoot()
        if (found != null) {
            if (found != child) {
                @Suppress("DEPRECATION") child.recycle()
            }
            return found
        } else {
            @Suppress("DEPRECATION") child.recycle()
        }
    }
    return null
}

/**
 * 从这个节点中向下寻找到第一个可选择的节点。
 * 返回的节点由调用者负责回收。
 */
fun AccessibilityNodeInfo.findCheckable(): AccessibilityNodeInfo? {
    if (isCheckable) return this
    for (i in 0 until childCount) {
        val child = getChild(i) ?: continue
        if (child.isCheckable) {
            return child
        }
        val found = child.findCheckable()
        if (found != null) {
            @Suppress("DEPRECATION") child.recycle()
            return found
        } else {
            @Suppress("DEPRECATION") child.recycle()
        }
    }
    return null
}

/**
 * 在当前活动窗口中查找包含指定文本的节点，如果页面可滑动则自动滚动查找。
 * 返回的节点由调用者负责回收。
 *
 * @param name 要查找的文本
 * @param delayTime 每次滑动后的等待时间（毫秒）
 * @param retryTimes 整个查找过程的重试次数（每次重试会从当前窗口重新开始查找，不会重置滚动位置）
 * @param scrollAttempts 每次查找允许的最大滚动次数
 */
suspend fun AccessibilityNodeInfo.findByScrollOrNull(
    name: String, delayTime: Long = 500L, retryTimes: Int = 1, scrollAttempts: Int = 20
): AccessibilityNodeInfo? {
    val server = AccessibilityServiceHelper.server ?: return null
    repeat(retryTimes) { retryIdx ->
        var root = this
        try {
            // 先在不滑动的情况下查找
            val first = root.findAccessibilityNodeInfosByText(name).firstOrNull()
            if (first != null) return first

            val scrollRoot = root.findScrollRoot() ?: return@repeat
            var maxScroll = scrollAttempts
            while (maxScroll-- > 0) {
                if (!scrollRoot.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)) break
                delay(delayTime)

                val newRoot = server.rootInActiveWindow ?: break
                @Suppress("DEPRECATION") root.recycle()
                root = newRoot

                val matchList = root.findAccessibilityNodeInfosByText(name)
                val match = matchList.firstOrNull()
                matchList.forEach { node ->
                    if (node != match) @Suppress("DEPRECATION") node.recycle()
                }
                if (match != null) {
                    @Suppress("DEPRECATION") scrollRoot.recycle()
                    return match
                }
            }
            @Suppress("DEPRECATION") scrollRoot.recycle()
        } finally {
            @Suppress("DEPRECATION") root.recycle()
        }
        // 重试前等待一段时间
        if (retryIdx < retryTimes - 1) delay(300L)
    }
    return null
}