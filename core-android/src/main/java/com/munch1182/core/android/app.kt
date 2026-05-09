package com.munch1182.core.android

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ApplicationInfo
import androidx.startup.Initializer
import com.munch1182.core.common.DefaultCoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext

/**
 * 一个实现了 AndroidX Initializer 接口的自定义初始化器类。
 * 该类负责使用应用程序上下文初始化 AppHelper。
 * 它是应用程序启动序列的一部分，确保正确初始化。
 */
class LibContextInitializer : Initializer<AppHelper> {
    override fun create(context: Context): AppHelper {
        AppHelper.manualInit(
            context.applicationContext as? Application ?: error("Application context is not an instance of Application")
        )
        return AppHelper
    }

    override fun dependencies() = emptyList<Class<out Initializer<*>>>()
}

interface AppProvider {
    val app: Application
}

/**
 * 代表 [Application] 上下文的全局单例。
 *
 * 1. 提供一个object的[Application]对象
 * 2. 提供一个[Application]范围的[CoroutineScope] (默认在[Dispatchers.IO])
 *
 * 调用顺序：
 * [Application.attachBaseContext]
 * [AppHelper.manualInit]
 * [Application.onCreate]
 */
object AppHelper : ContextWrapper(null), AppProvider, CoroutineScope {

    @Volatile
    private var initialized = false
    private lateinit var scopeContext: CoroutineContext

    /**
     * 返回绑定的 [Application]
     *
     * 在[Application.onCreate]之前调用会抛出异常[IllegalStateException]
     **/
    override val app: Application
        get() {
            checkInitialized()
            return this.applicationContext as Application // 如果已经初始化, 此处必定转换成功
        }

    /**
     * 返回是否的debug模式
     *
     * 在[Application.onCreate]之前调用会抛出异常[IllegalStateException]
     **/
    val isDebug: Boolean
        get() {
            checkInitialized()
            return applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        }

    @Synchronized
    internal fun manualInit(app: Application) {
        if (initialized) return
        attachBaseContext(app)
        scopeContext = SupervisorJob() + Dispatchers.IO + DefaultCoroutineExceptionHandler(Log)
        initialized = true
    }

    private fun checkInitialized() {
        check(initialized) { "AppHelper has not been initialized. Make sure manualInit() was called." }
    }

    override val coroutineContext: CoroutineContext
        get() {
            checkInitialized()
            return scopeContext
        }
}
