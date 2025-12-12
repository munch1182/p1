package com.munch1182.android.lib.helper


import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.munch1182.android.lib.base.OnUpdateListener
import com.munch1182.android.lib.base.log
import java.lang.ref.WeakReference
import java.util.Stack

open class DefaultActivityLifecycleCallbacks : Application.ActivityLifecycleCallbacks {
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
    }

    override fun onActivityStarted(activity: Activity) {
    }

    override fun onActivityResumed(activity: Activity) {
    }

    override fun onActivityPaused(activity: Activity) {
    }

    override fun onActivityStopped(activity: Activity) {
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
    }

    override fun onActivityDestroyed(activity: Activity) {
    }
}

/**
 * 当前activity的管理工具类
 */
/**
 * 增强的Activity管理工具类
 * 支持Activity栈管理、前台状态判断等功能
 */
object ActivityCurrHelper : ARManager<OnUpdateListener<Boolean>> by ARDefaultManager() {

    private val activityStack = Stack<WeakReference<Activity>>()
    private var resumedActivityCount = 0 // 记录处于resumed状态的Activity数量
    private var isAppInForeground = false // 应用是否在前台

    private val log = this.log(false)

    // 当前活动的Activity
    val curr: Activity?
        get() = activityStack.lastOrNull { it.get()?.isFinishing == false }?.get()

    // 栈中存活的Activity数量
    val activityCount: Int
        get() = activityStack.count { it.get()?.isFinishing == false }

    // 应用是否在前台（有Activity处于resumed状态）
    val isAppForeground: Boolean
        get() = isAppInForeground

    // 当前Activity是否在前台（栈顶且resumed）
    val isCurrentActivityForeground: Boolean
        get() = isAppInForeground && resumedActivityCount > 0

    private val callback by lazy {
        object : DefaultActivityLifecycleCallbacks() {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                super.onActivityCreated(activity, savedInstanceState)
                // 清理已销毁的Activity引用
                cleanupDestroyedActivities()
                activityStack.push(WeakReference(activity))
                log.logStr("Activity created: ${activity::class.java.simpleName}, stack size: $activityCount")
            }

            override fun onActivityResumed(activity: Activity) {
                super.onActivityResumed(activity)
                resumedActivityCount++
                updateForegroundStatus()
                log.logStr("Activity resumed: ${activity::class.java.simpleName}, resumed count: $resumedActivityCount")
            }

            override fun onActivityPaused(activity: Activity) {
                super.onActivityPaused(activity)
                resumedActivityCount = maxOf(0, resumedActivityCount - 1)
                updateForegroundStatus()
                log.logStr("Activity paused: ${activity::class.java.simpleName}, resumed count: $resumedActivityCount")
            }

            override fun onActivityDestroyed(activity: Activity) {
                super.onActivityDestroyed(activity)
                activityStack.removeAll { it.get() == activity || it.get() == null }
                log.logStr("Activity destroyed: ${activity::class.java.simpleName}, stack size: $activityCount")
            }
        }
    }

    /**
     * 更新应用前台状态
     */
    private fun updateForegroundStatus() {
        val wasInForeground = isAppInForeground
        isAppInForeground = resumedActivityCount > 0

        // 状态变化时通知监听器
        if (wasInForeground != isAppInForeground) {
            log.logStr("App foreground status changed: $isAppInForeground")
            forEach { it.onUpdate(isAppForeground) }
        }
    }

    /**
     * 清理已销毁的Activity引用
     */
    private fun cleanupDestroyedActivities() {
        activityStack.removeAll { it.get() == null || it.get()?.isFinishing == true }
    }

    /**
     * 注册Activity生命周期回调
     */
    fun register(app: Application) {
        app.registerActivityLifecycleCallbacks(callback)
    }

    /**
     * 注销Activity生命周期回调
     */
    fun unregister(app: Application) {
        app.unregisterActivityLifecycleCallbacks(callback)
    }

    /**
     * 获取栈中所有存活的Activity
     */
    fun getAliveActivities(): List<Activity> {
        return activityStack.mapNotNull { it.get() }.filter { !it.isFinishing }
    }

    /**
     * 根据类名查找Activity
     */
    fun findActivityByClass(activityClass: Class<out Activity>): Activity? {
        return getAliveActivities().find { it::class.java == activityClass }
    }

    /**
     * 判断指定Activity是否在栈中
     */
    fun containsActivity(activityClass: Class<out Activity>): Boolean {
        return findActivityByClass(activityClass) != null
    }

    /**
     * 判断指定Activity是否是当前栈顶Activity
     */
    fun isActivityOnTop(activityClass: Class<out Activity>): Boolean {
        return curr?.let { it::class.java == activityClass } ?: false
    }

    /**
     * 获取栈信息（用于调试）
     */
    fun getStackInfo(): String {
        val activities = getAliveActivities()
        return "Stack size: ${activities.size}, " + "Foreground: $isAppInForeground, " + "Resumed count: $resumedActivityCount, " + "Activities: ${activities.map { it::class.java.simpleName }}"
    }

    /**
     * 重启应用
     */
    fun restartApp(restartTo: Class<out Activity>, fromActivity: Activity? = curr) {
        fromActivity?.let { activity ->
            val intent = Intent(activity, restartTo).apply {
                setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            activity.startActivity(intent)
            activity.finish()
        }
    }
}

// 扩展属性
val currAct: Activity
    get() = ActivityCurrHelper.curr ?: throw IllegalStateException("No active activity found. Make sure ActivityCurrHelper is registered.")

val currAsFM: FragmentActivity
    get() = ActivityCurrHelper.curr as? FragmentActivity ?: throw IllegalStateException("Current activity is not a FragmentActivity")

