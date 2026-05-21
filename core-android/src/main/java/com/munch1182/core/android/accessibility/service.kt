package com.munch1182.core.android.accessibility

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration

/**
 * 无障碍服务;
 *
 * 需要先注册:
 * ```XML
 * <service
 * android:name=".*AccessibilityService"
 *  android:exported="false"
 *  android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"
 *   tools:ignore="AccessibilityPolicy">
 *   <intent-filter>
 *   <action android:name="android.accessibilityservice.AccessibilityService" />
 *   </intent-filter>
 *    <meta-data
 *     android:name="android.accessibilityservice"
 *     android:resource="@xml/accessibility_service_config" />
 * </service>
 * ```
 *
 * res/xml/accessibility_service_config.xml:
 * ```XML
 * <?xml version="1.0" encoding="utf-8"?>
 * <accessibility-service xmlns:android="http://schemas.android.com/apk/res/android"
 *     android:description="@string/accessibility_service_description"
 *     android:accessibilityEventTypes="typeAllMask"
 *     android:accessibilityFeedbackType="feedbackGeneric"
 *     android:accessibilityFlags="flagDefault"
 *     android:canRetrieveWindowContent="true"
 *     android:notificationTimeout="100"
 *     android:packageNames="com.android.settings"  />
 * ```
 */
@SuppressLint("AccessibilityPolicy")
abstract class BaseAccessibilityService : AccessibilityService() {
    private val _events = MutableSharedFlow<AccessibilityEvent?>(extraBufferCapacity = 16)
    val events = _events.asSharedFlow()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    init {
        AccessibilityServiceHelper.init(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        serviceScope.launch { _events.emit(event) }
    }

    override fun onInterrupt() {
    }

    override fun onDestroy() {
        AccessibilityServiceHelper.release()
        serviceScope.cancel()
        super.onDestroy()
    }
}

/**
 * 匹配需要的事件
 */
typealias EventMatcher = suspend (AccessibilityEvent?, AccessibilityService) -> Boolean

/**
 * 无障碍服务处理辅助类
 */
object AccessibilityServiceHelper {
    private var _server: BaseAccessibilityService? = null

    /**
     * 返回一个[AccessibilityService]对象, 如果不存在, 返回null
     */
    val server: AccessibilityService? get() = _server
    internal fun init(server: BaseAccessibilityService) {
        this._server = server
    }

    internal fun release() {
        _server = null
    }

    /**
     * 等待无障碍服务回调的事件流
     */
    suspend fun awaitEvent(timeout: Duration = Duration.ZERO, match: EventMatcher): AccessibilityEvent? {
        val service = _server ?: return null
        val flow = service.events.filter { match(it, service) }.take(1)
        return if (timeout == Duration.ZERO) flow.firstOrNull()
        else withTimeoutOrNull(timeout) { flow.firstOrNull() }
    }

    /**
     * 接受无障碍服务回调的事件流
     */
    fun collectEvent(predicate: EventMatcher) = _server?.let { server ->
        server.events.filter { predicate(it, server) }
    }
}

/**
 * 匹配事件类型
 */
fun eventType(vararg type: Int): EventMatcher =
    { event, _ -> event?.eventType?.let { it in type } ?: false }

/**
 * 匹配事件类型: 窗口状态改变
 */
fun windowType() = eventType(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)

/**
 * 匹配包名
 */
fun packageName(vararg name: String): EventMatcher =
    { event, _ -> event?.packageName?.let { it in name } ?: false }

/**
 * 匹配类名
 */
fun className(vararg name: String): EventMatcher =
    { event, _ -> event?.className?.let { it in name } ?: false }

/**
 * 合并匹配: 需要同时匹配才算匹配
 */
infix fun EventMatcher.and(other: EventMatcher): EventMatcher =
    { event, service -> this(event, service) && other(event, service) }

/**
 * 合并匹配: 任一匹配则算匹配
 */
infix fun EventMatcher.or(other: EventMatcher): EventMatcher =
    { event, service -> this(event, service) || other(event, service) }

/**
 * 合并匹配: 需要同时匹配才算匹配
 */
operator fun EventMatcher.plus(matcher: EventMatcher): EventMatcher = this and matcher