package com.munch1182.lib.helper


import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.munch1182.lib.base.log
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
object ActivityCurrHelper {

    private val activityStack = Stack<WeakReference<Activity>>()
    private val log = this.log(false)

    // 当前活动的Activity
    val curr: Activity?
        get() = activityStack.lastOrNull { it.get()?.isFinishing == false }?.get()

    // 应用是否在前台
    val isAppInForeground: Boolean get() = activityStack.isNotEmpty()

    // 栈中Activity数量
    val activityCount: Int get() = activityStack.count { it.get()?.isFinishing == false }

    private val callback by lazy {
        object : DefaultActivityLifecycleCallbacks() {
            override fun onActivityStarted(activity: Activity) {
                super.onActivityStarted(activity)
                // 移除已销毁的Activity引用
                activityStack.removeAll { it.get() == null || it.get()?.isFinishing == true }
                // 如果Activity已经在栈中，先移除再重新添加（保证在栈顶）
                activityStack.removeAll { it.get() == activity }
                activityStack.push(WeakReference(activity))
                log.logStr("Activity started: ${activity::class.java.simpleName}, stack size: $activityCount")
            }

            override fun onActivityStopped(activity: Activity) {
                super.onActivityStopped(activity)
                // 不移除，只在destroy时移除
                log.logStr("Activity stopped: ${activity::class.java.simpleName}")
            }

            override fun onActivityDestroyed(activity: Activity) {
                super.onActivityDestroyed(activity)
                activityStack.removeAll { it.get() == activity || it.get() == null }
                log.logStr("Activity destroyed: ${activity::class.java.simpleName}, stack size: $activityCount")
            }
        }
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
     * 结束指定Activity
     */
    fun finishActivity(activityClass: Class<out Activity>) {
        findActivityByClass(activityClass)?.finish()
    }

    /**
     * 结束除指定Activity外的所有Activity
     */
    fun finishAllActivitiesExcept(activityClass: Class<out Activity>) {
        getAliveActivities().forEach { activity ->
            if (activity::class.java != activityClass) {
                activity.finish()
            }
        }
    }

    /**
     * 结束所有Activity
     */
    fun finishAllActivities() {
        getAliveActivities().forEach { it.finish() }
    }

    /**
     * 返回到指定Activity
     */
    fun backToActivity(activityClass: Class<out Activity>): Boolean {
        val activities = getAliveActivities()
        val targetIndex = activities.indexOfFirst { it::class.java == activityClass }

        if (targetIndex == -1) return false

        // 结束目标Activity之后的所有Activity
        for (i in targetIndex + 1 until activities.size) {
            activities[i].finish()
        }

        return true
    }

    /**
     * 安全启动Activity
     */
    fun startActivitySafely(intent: Intent, fromActivity: Activity? = curr) {
        fromActivity?.let { activity ->
            try {
                activity.startActivity(intent)
            } catch (e: Exception) {
                log.logStr("Start activity failed: ${e.message}")
                // 添加FLAG_ACTIVITY_NEW_TASK重试
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                activity.startActivity(intent)
            }
        }
    }

    /**
     * 重启应用
     */
    fun restartApp(restartTo: Class<out Activity>, fromActivity: Activity? = curr) {
        fromActivity?.let { activity ->
            val intent = Intent(activity, restartTo).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK }
            activity.startActivity(intent)
            // 结束当前Activity
            activity.finish()
        }
    }
}

// 扩展属性
val currAct: Activity
    get() = ActivityCurrHelper.curr
        ?: throw IllegalStateException("No active activity found. Make sure ActivityCurrHelper is registered.")

val currAsFM: FragmentActivity
    get() = ActivityCurrHelper.curr as? FragmentActivity
        ?: throw IllegalStateException("Current activity is not a FragmentActivity")

// 扩展函数
/**
 * 重启应用到指定Activity
 */
fun restartApp(restartTo: Class<out Activity>) {
    ActivityCurrHelper.restartApp(restartTo)
}

/**
 * 安全启动Activity
 */
fun startActivitySafely(intent: Intent) {
    ActivityCurrHelper.startActivitySafely(intent)
}

/**
 * 结束所有Activity
 */
fun finishAllActivities() {
    ActivityCurrHelper.finishAllActivities()
}

/**
 * 判断指定Activity是否在栈中
 */
fun isActivityInStack(activityClass: Class<out Activity>): Boolean {
    return ActivityCurrHelper.containsActivity(activityClass)
}