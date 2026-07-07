package com.munch1182.core.router


private const val SCHEME = "p1app"
private const val HOST = "navigate"

/**
 * 跨模块 DeepLink 路由字符串定义。
 *
 * 此模块为纯 Kotlin/JVM、无 Android 依赖，编译速度远快于 [core-ui]，
 * 避免路由变更触发 Compose/资源级联重编译。
 *
 * ## 声明端
 * ```
 * @Destination<...>(deepLinks = [DeepLink(DeepLinkRoutes.BLUETOOTH.CONNECT_URI_TEMPLATE)])
 * internal fun BluetoothConnectScreen(mac: String, name: String) { ... }
 * ```
 *
 * ## 调用端
 * ```
 * // 方式一：NavController 内存路由（不经过系统 intent-filter）
 * navController.handleDeepLink(Uri.parse(DeepLinkRoutes.BLUETOOTH.connectUri(mac, name)))
 *
 * // 方式二：startActivity（走系统路由，启动注册了 intent-filter 的 Activity）
 * startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(DeepLinkRoutes.BLUETOOTH.connectUri(mac, name))))
 * ```
 *
 * ## AndroidManifest 注册（方式二需要）
 * ```
 * <activity android:name=".DeepLinkActivity">
 *     <intent-filter>
 *         <action android:name="android.intent.action.VIEW" />
 *         <category android:name="android.intent.category.DEFAULT" />
 *         <category android:name="android.intent.category.BROWSABLE" />
 *         <data android:scheme="p1app" android:host="navigate" />
 *     </intent-filter>
 * </activity>
 * ```
 *
 * ## Activity 中解析参数
 * ```
 * class DeepLinkActivity : BaseActivity() {
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *         super.onCreate(savedInstanceState)
 *         val uri = intent?.data ?: return
 *         val mac = uri.getQueryParameter("mac")
 *         navController.handleDeepLink(intent)
 *     }
 * }
 * ```
 */
object DeepLinkRoutes {

    object BLUETOOTH {
        private const val PATH = "bluetooth"
        const val SCAN_URI = "$SCHEME://$HOST/$PATH"
        const val CONNECT_URI_TEMPLATE = "$SCHEME://$HOST/$PATH/connect?mac={mac}&name={name}"
        fun connectUri(mac: String, name: String): String =
            "$SCHEME://$HOST/$PATH/connect?mac=$mac&name=$name"
    }

    object AUDIO {
        private const val PATH = "audio"
        const val HOME_URI = "$SCHEME://$HOST/$PATH"
    }

    object BROWSER {
        private const val PATH = "browser"
        const val HOME_URI = "$SCHEME://$HOST/$PATH"
    }
}
