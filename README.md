# Android Core Library 设计文档

## 1. 设计理念
- **开箱即用**：新项目引入 `core` 模块即可拥有全部基础设施。
- **接口隔离**：业务代码只依赖抽象接口，不依赖具体实现。
- **可替换**：通过依赖注入可以无痛替换任何模块的实现（如从 Firebase 切换到 Mixpanel）。
- **合理合并**：不过度拆分模块，降低版本管理成本和认知负担。

## 2. 模块划分
```
:core            // 地基模块（必选）
:core-ui         // UI 通用模块（可选）
:core-services   // 高级业务服务模块（可选）
```

### 2.1 模块依赖关系
- `core` 无外部依赖（仅依赖 Kotlin / AndroidX 部分基础库）。
- `core-ui` 依赖 `core`。
- `core-services` 依赖 `core`，按需可选依赖 `core-ui`。

---

## 3. 地基模块 `:core` 详细设计
**包含内容**：所有 P0 优先级的基础能力，适合任何类型的应用。

### 3.1 统一日志接口 `AppLogger`
```kotlin
interface AppLogger {
    fun log(priority: LogPriority, tag: String, message: String, throwable: Throwable? = null)
    fun v(tag: String, message: String, throwable: Throwable? = null)
    fun d(tag: String, message: String, throwable: Throwable? = null)
    fun i(tag: String, message: String, throwable: Throwable? = null)
    fun w(tag: String, message: String, throwable: Throwable? = null)
    fun e(tag: String, message: String, throwable: Throwable? = null)
}

enum class LogPriority { VERBOSE, DEBUG, INFO, WARN, ERROR }
```
- **默认实现**：`DefaultAppLogger`，内部使用 `android.util.Log`。
- **用途**：统一项目日志，后续可替换为 `Timber` 或直接上报到 Crashlytics。

### 3.2 埋点分析接口 `AnalyticsTracker`
```kotlin
interface AnalyticsTracker {
    fun trackScreen(screenName: String, properties: Map<String, Any>? = null)
    fun trackEvent(eventName: String, properties: Map<String, Any>? = null)
    fun trackUserProperty(key: String, value: Any)
    fun identify(userId: String, traits: Map<String, Any>? = null)
    fun setUserProperties(properties: Map<String, Any>)
}
```
- **默认实现**：`NoOpAnalyticsTracker`（空实现）。
- **用途**：预留用户行为分析入口，初期不接入任何 SDK 也能正常编译运行。

### 3.3 全局错误处理 `ErrorHandler`
```kotlin
interface ErrorHandler {
    fun handleException(throwable: Throwable, context: ErrorContext? = null)
}

data class ErrorContext(
    val screen: String? = null,
    val action: String? = null,
    val extra: Map<String, Any> = emptyMap()
)
```
- **默认实现**：`DefaultErrorHandler`，内部调用 `AppLogger` 打印错误日志。
- **用途**：统一捕获未处理异常，后续可对接 `Crashlytics` 等。

### 3.4 网络状态监控 `NetworkMonitor`
```kotlin
interface NetworkMonitor {
    val isConnected: StateFlow<Boolean>
    val networkType: StateFlow<NetworkType>

    fun startMonitoring()
    fun stopMonitoring()
}

enum class NetworkType { WIFI, CELLULAR, ETHERNET, UNKNOWN, NONE }
```
- **默认实现**：`ConnectivityManagerNetworkMonitor`，基于 `ConnectivityManager`。
- **用途**：全局网络状态观察，方便离线提示或缓存策略。

### 3.5 协程调度器 `DispatcherProvider`
```kotlin
interface DispatcherProvider {
    val main: CoroutineDispatcher
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
    val unconfined: CoroutineDispatcher
}
```
- **默认实现**：`DefaultDispatcherProvider`，映射到 `Dispatchers.Main/Io/Default/Unconfined`。
- **用途**：方便单元测试替换为 `TestDispatcher`。

### 3.6 键值存储 `PreferencesManager`
```kotlin
interface PreferencesManager {
    fun <T> put(key: String, value: T)
    fun <T> get(key: String, defaultValue: T): T
    fun remove(key: String)
    fun clear()
    fun observe(key: String): Flow<Any?>
}
```
- **默认实现**：`DataStorePreferencesManager`，基于 `DataStore<Preferences>`。
- **用途**：统一偏好读写，支持响应式观察。

### 3.7 安全存储 `SecureStorage`
```kotlin
interface SecureStorage {
    fun saveEncrypted(key: String, value: String)
    fun getDecrypted(key: String): String?
    fun remove(key: String)
    fun clear()
}
```
- **默认实现**：`EncryptedSharedPreferencesStorage`，基于 AndroidX Security 的 `EncryptedSharedPreferences`。
- **用途**：存储 Token、密钥等敏感信息。

### 3.8 通用数据包装 `Resource`
```kotlin
sealed class Resource<out T> {
    data class Success<T>(val data: T) : Resource<T>()
    data class Error(val throwable: Throwable, val cachedData: Any? = null) : Resource<Nothing>()
    object Loading : Resource<Nothing>()
}
```
- **用途**：统一表示网络/数据库加载状态。

### 3.9 应用前后台监听 `AppLifecycleTracker`
```kotlin
interface AppLifecycleTracker {
    val isInForeground: StateFlow<Boolean>
}
```
- **默认实现**：基于 `ProcessLifecycleOwner`。
- **用途**：判断用户是否在使用应用，控制后台任务。

### 3.10 扩展工具
- 日期/时间格式化扩展
- 数字/货币格式化扩展
- Context 扩展（如 `Context.appLogger`）
- 常见 intent 构建器

---

## 4. UI 通用模块 `:core-ui` 详细设计
**包含内容**：可复用的 UI 组件和界面相关抽象。**如果不维护统一组件库，此模块可不创建。**

### 4.1 主题管理 `ThemeManager`
```kotlin
interface ThemeManager {
    val currentTheme: StateFlow<ThemeMode>
    fun setTheme(mode: ThemeMode)
    fun toggleDarkMode()
}

enum class ThemeMode { LIGHT, DARK, SYSTEM }
```
- **默认实现**：`AppCompatThemeManager`，基于 `AppCompatDelegate.setDefaultNightMode`。
- **用途**：统一管理暗黑模式，可在设置页一键切换。

### 4.2 图片加载 `ImageLoader`
```kotlin
interface ImageLoader {
    fun load(url: String, imageView: ImageView, options: ImageOptions? = null)
    fun preload(url: String)
    fun clearMemoryCache()
    fun clearDiskCache()
}

data class ImageOptions(
    val placeholder: Drawable? = null,
    val error: Drawable? = null,
    val size: Size? = null,
    val roundedCorners: Float? = null,
    val circle: Boolean = false
)
```
- **默认实现**：`CoilImageLoader`，内部使用 Coil。
- **用途**：统一图片加载入口，方便未来替换 Glide 或 Fresco。

### 4.3 权限请求 `PermissionRequester`
```kotlin
interface PermissionRequester {
    suspend fun request(permission: Permission): PermissionResult
    suspend fun requestMultiple(vararg permissions: Permission): Map<Permission, PermissionResult>
    fun isGranted(permission: Permission): Boolean
    fun shouldShowRationale(permission: Permission): Boolean
}

enum class Permission { CAMERA, LOCATION, STORAGE, PHONE, MICROPHONE }
sealed class PermissionResult {
    object Granted : PermissionResult()
    object Denied : PermissionResult()
    object DeniedPermanently : PermissionResult()
}
```
- **默认实现**：基于 `ActivityResultContracts.RequestPermission`。
- **用途**：简化运行时权限请求，支持协程。

### 4.4 通知管理器 `NotificationHelper`
```kotlin
interface NotificationHelper {
    fun createChannel(channel: ChannelConfig)
    fun showNotification(id: Int, title: String, body: String, channelId: String)
    fun cancel(id: Int)
}
```
- **默认实现**：`AndroidNotificationHelper`，封装 `NotificationManagerCompat`。
- **用途**：统一通知渠道管理，避免遗漏适配。

### 4.5 基础 UI 组件
- `LoadingView`（可自定义的加载动画）
- `ErrorView`（重试按钮 + 错误提示）
- `EmptyView`（空占位图）
- `ConfirmDialog`（通用确认对话框）

---

## 5. 高级服务模块 `:core-services` 详细设计
**包含内容**：P2 优先级的高级功能，按需引入。

### 5.1 支付抽象 `PaymentManager`
```kotlin
interface PaymentManager {
    suspend fun requestPayment(order: PaymentOrder): PaymentResult
}
```
- **默认实现**：暂无，需宿主接入具体支付 SDK 实现。
- **用途**：统一支付入口，切换支付平台时业务代码不动。

### 5.2 社交分享 `ShareManager`
```kotlin
interface ShareManager {
    suspend fun share(content: ShareContent, channel: ShareChannel): ShareResult
}
```
- **默认实现**：`SystemShareManager`，调用系统分享 Intent。
- **用途**：支持第三方分享 SDK 替换。

### 5.3 离线数据管理 `OfflineFirstRepository`
```kotlin
interface OfflineFirstRepository<T> {
    fun observeData(): Flow<Resource<T>>
    suspend fun refresh()
}
```
- **默认实现**：提供抽象基类 `BaseOfflineFirstRepository`，使用 `NetworkBoundResource` 模式。
- **用途**：统一本地/远程数据同步逻辑。

### 5.4 功能开关 `FeatureFlagManager`
```kotlin
interface FeatureFlagManager {
    fun isEnabled(flag: String, defaultValue: Boolean): Boolean
    fun observe(flag: String): Flow<Boolean>
    suspend fun refresh()
}
```
- **默认实现**：`FirebaseRemoteConfigManager` 或 `LocalFeatureFlagManager`。
- **用途**：实现灰度发布、A/B 测试。

### 5.5 性能监控 `PerformanceMonitor`
```kotlin
interface PerformanceMonitor {
    fun trackColdStart(duration: Long)
    fun trackPageLoad(page: String, duration: Long)
    fun trackNetworkRequest(url: String, duration: Long, statusCode: Int)
}
```
- **默认实现**：`NoOpPerformanceMonitor`，接入时替换为 Firebase Performance 等。

---

## 6. 依赖注入与使用方式
### 6.1 依赖注入模块示例（Hilt）
每个 `core` 模块提供自己的 Hilt Module，例如：
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object CoreModule {
    @Provides @Singleton
    fun provideAppLogger(): AppLogger = DefaultAppLogger()

    @Provides @Singleton
    fun provideAnalyticsTracker(): AnalyticsTracker = NoOpAnalyticsTracker()

    @Provides @Singleton
    fun provideDispatcherProvider(): DispatcherProvider = DefaultDispatcherProvider()
    // ...
}
```
使用者只需在 Application 类上添加 `@HiltAndroidApp` 即可自动注入。

### 6.2 替换默认实现
如果要接入 Firebase：
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {
    @Provides @Singleton
    fun provideAnalyticsTracker(): AnalyticsTracker =
        FirebaseAnalyticsTracker(firebaseAnalytics)

    @Provides @Singleton
    fun provideErrorHandler(appLogger: AppLogger): ErrorHandler =
        CrashlyticsErrorHandler(appLogger)
}
```

### 6.3 在业务代码中使用
```kotlin
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject lateinit var logger: AppLogger
    @Inject lateinit var analytics: AnalyticsTracker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        analytics.trackScreen("MainActivity")
        logger.d("MainActivity", "onCreate")
    }
}
```

---

## 7. 最终建议
- **80% 的项目**：只需引入 `:core` 模块。
- **有跨应用统一 UI 规范**：额外引入 `:core-ui`。
- **需要支付、分享、离线缓存等高级功能**：再引入 `:core-services`。
- 所有默认实现都使用 **Android 原生 API** 或 **no-op**，做到即时运行，无第三方 SDK 绑定。