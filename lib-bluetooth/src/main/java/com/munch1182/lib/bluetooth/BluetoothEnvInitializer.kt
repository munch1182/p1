package com.munch1182.lib.bluetooth

import android.content.Context
import androidx.startup.Initializer
import com.munch1182.lib.android.LibContextInitializer

/**
 * [BluetoothEnv]的确定性初始化器, 通过[androidx.startup][Initializer]在ContentProvider阶段触发初始化.
 *
 * ## 为什么不依赖[BluetoothEnv]的[object init][BluetoothEnv]
 *
 * [BluetoothEnv]是Kotlin `object`, `init {}`在首次访问时执行, 时机不可控:
 * - 若首次访问发生在UI渲染路径(Composable/ViewModel), 会阻塞冷启动关键帧
 * - 若首次访问发生在后台线程, `init {}`内的[BroadcastReceiver注册][BluetoothReceiver.register]存在线程隐患
 *
 * 使用[Initializer]将初始化提前到ContentProvider阶段, 保证:
 * - 确定性执行时机(Application.onCreate之前)
 * - 始终在主线程执行
 * - 不占用UI冷启动路径
 *
 * ## 调用时序
 *
 * ```
 * Application.attachBaseContext()
 *     ↓
 * LibContextInitializer.create()        ← AppHelper 就绪
 *     ↓
 * BluetoothEnvInitializer.create()     ← BluetoothEnv.init {} 触发
 *     │                                     ├─ BluetoothReceiver 注册
 *     │                                     └─ onOffState 开始收集 (SharingStarted.Eagerly)
 *     ↓
 * Application.onCreate()
 *     ↓
 * 任何代码安全使用 BluetoothEnv         ← 已完全就绪
 * ```
 *
 * @see BluetoothEnv
 * @see LibContextInitializer
 */
class BluetoothEnvInitializer : Initializer<BluetoothEnv> {
    override fun create(context: Context): BluetoothEnv {
        return BluetoothEnv
    }

    override fun dependencies() = listOf(LibContextInitializer::class.java)
}
