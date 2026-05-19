package com.munch1182.core.android

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.content.ComponentName
import android.content.Context
import android.graphics.Path
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityNodeInfo
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
    val targetComponent = ComponentName(ctx, clazz).flattenToShortString() // 与系统格式保持一致
    Log.d("aaa", targetComponent)
    for (info in list) {
        Log.d("aaa", "info: ${info.id}, $info")
    }
    return list.any { it.id == targetComponent }
}

/**
 * 点击该node; 如果该node可点击, 则直接调用点击; 否则获取该node的位置使用手势模拟点击;
 *
 * 注意: 如果有动画, 要给动画留出足够的时间才能进行点击.
 * 需要在配置中设置可模拟手势
 *
 * @return 是否已经执行过点击, 如果执行, 调用者要自己预留动画时间
 *
 * ```xml
 * <accessibility-service
 * ...
 * android:canPerformGestures="true"
 * />
 * ```
 */
fun AccessibilityNodeInfo.click(server: AccessibilityService): Boolean {
    val node = this
    if (!node.isVisibleToUser) return false

    if (node.isClickable && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
        return true
    }
    val rect = android.graphics.Rect()
    node.getBoundsInScreen(rect)

    if (rect.isEmpty) return false

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

/**
 * 从这个节点中向下寻找到第一个允许滑动的节点
 *
 * 注意: 如果是复杂页面有多个可滑动元素, 要对找到的元素进行验证
 */
fun AccessibilityNodeInfo.findScrollRoot(): AccessibilityNodeInfo? {
    val node = this
    if (node.isScrollable) return node
    for (i in 0 until node.childCount) {
        val child = node.getChild(i)
        if (child != null) {
            val found = child.findScrollRoot()
            if (found != null) return found
        }
    }
    return null
}

/**
 * 从这个节点中向下寻找到第一个可选择的节点
 */
fun AccessibilityNodeInfo.findCheckable(): AccessibilityNodeInfo? {
    if (isCheckable) return this
    for (i in 0 until childCount) {
        val child = getChild(i)
        if (child != null) {
            if (child.isCheckable) return child
            val found = child.findCheckable()
            if (found != null) return found
        }
    }
    return null
}

/**
 * 从这个节点中向下寻找到第一个包含指定文本的节点, 如果页面可滑动, 还会尝试向下滑动页面来查找该元素
 *
 * @see AccessibilityNodeInfo.findAccessibilityNodeInfosByText
 */
suspend fun AccessibilityService.findByScrollOrNull(name: String, delayTime: Long = 500L): AccessibilityNodeInfo? {
    val root = rootInActiveWindow ?: return null
    val first = root.findAccessibilityNodeInfosByText(name).firstOrNull()
    if (first != null) return first

    val scrollRoot = root.findScrollRoot() ?: return null // 不可滑动

    var maxScrollAttempts = 20   // 防止死循环

    while (maxScrollAttempts-- > 0) {
        if (!scrollRoot.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)) {
            // 滚动动作失败（可能已到底部）
            break
        }
        delay(delayTime)

        val currentRoot = rootInActiveWindow ?: break
        val match = currentRoot.findAccessibilityNodeInfosByText(name).firstOrNull()
        if (match != null) return match
    }
    return null
}