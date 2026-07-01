# Compose Destinations v2 技术指南

> 基于 [Compose Destinations v2.3.0](https://composedestinations.rafaelcosta.xyz/v2/) 官方文档与工程实践

---

## 1. 单模块使用

### 1.1 Gradle 依赖配置

每个使用 Compose Destinations 的模块都需要添加：

```kotlin
// build.gradle.kts
plugins {
    alias(libs.plugins.google.ksp)
}

dependencies {
    implementation(libs.compose.destinations)      // 运行时库
    ksp(libs.compose.destinations.ksp)             // KSP 代码生成器
    implementation(libs.androidx.navigation3.ui)   // Compose Navigation 基础
    implementation(libs.androidx.navigation3.runtime)
}
```

KSP 可选配置：

```kotlin
ksp {
    // 指定模块名，避免生成类命名冲突（多模块推荐）
    arg("compose-destinations.moduleName", "featureX")
    // 生成 mermaid 可视化导航图
    arg("compose-destinations.mermaidGraph", "$rootDir/docs")
    arg("compose-destinations.htmlMermaidGraph", "$rootDir/docs")
}
```

### 1.2 默认使用——`@Destination<RootGraph>`

最简单的用法：直接使用库内置的 `RootGraph` 作为导航图。

```kotlin
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph

// 起点屏幕
@Destination<RootGraph>(start = true)
@Composable
fun HomeScreen(navigator: DestinationsNavigator) {
    // navigator.navigate(AboutScreenDestination)
}

// 普通屏幕
@Destination<RootGraph>
@Composable
fun AboutScreen() { /*...*/ }
```

在 Activity 中挂载：

```kotlin
DestinationsNavHost(navGraph = NavGraphs.root)  // NavGraphs 是 KSP 生成的
```

KSP 自动生成的结构：
- `NavGraphs.kt` → `NavGraphs.root`（聚合所有 `@Destination<RootGraph>` 的目的地）
- `navgraphs/RootNavGraph.kt` → 根导航图，包含 `destinations` 列表和 `startRoute`
- `destinations/HomeScreenDestination.kt` → 每个 `@Destination` 生成一个 `Destination` 对象

### 1.3 改名使用——自定义 `@NavHostGraph`

当需要更好的语义或要在多个 `DestinationsNavHost` 场景中区分时，创建自定义 NavHostGraph：

```kotlin
import com.ramcosta.composedestinations.annotation.NavHostGraph

@NavHostGraph
annotation class AppGraph

@Destination<AppGraph>(start = true)
@Composable
fun HomeScreen(navigator: DestinationsNavigator) { /*...*/ }

@Destination<AppGraph>
@Composable
fun SettingsScreen() { /*...*/ }
```

使用生成的 NavGraph：

```kotlin
// 生成 NavGraphs.app = AppNavGraph，直接用
DestinationsNavHost(navGraph = NavGraphs.app)
```

**注意**：`NavGraphs` 对象的属性名由 annotation 类名自动推导——`AppGraph` → `app`，`HomeGraph` → `home`。

### 1.4 多个导航图（嵌套 NavGraph）

复杂场景可以定义嵌套导航图：

```kotlin
// 嵌套导航图：SettingsGraph 的父级是 AppGraph
@NavGraph<AppGraph>
annotation class SettingsGraph

// Settings 子图中再嵌套
@NavGraph<SettingsGraph>
annotation class ProfileSettingsGraph

// 目的地归属
@Destination<SettingsGraph>(start = true)
@Composable
fun SettingsMainScreen() { /*...*/ }

@Destination<ProfileSettingsGraph>(start = true)
@Composable
fun ProfileSettingsScreen() { /*...*/ }
```

生成导航结构：
```
AppNavGraph
├── HomeScreen (start)
└── SettingsGraph (嵌套)
      ├── SettingsMainScreen (start)
      └── ProfileSettingsGraph (嵌套)
            └── ProfileSettingsScreen (start)
```

**Key Point**：每个 `@NavGraph` 组是编译时强校验的。每个图必须有且仅有一个 `start = true` 的目的地，编译期会检查。

### 1.5 参数传递

#### 方式一：直接在 Composable 参数中声明

```kotlin
@Destination<RootGraph>
@Composable
fun ProfileScreen(
    id: Int,                     // 必传参数（无默认值）
    name: String? = null,        // 可选参数（可空）
    navigator: DestinationsNavigator,  // navigator 不会被当作导航参数
) {
    // 直接使用参数
}
```

#### 方式二：使用 `navArgs` 数据类（推荐用于 ViewModel 场景）

```kotlin
data class ProfileScreenNavArgs(
    val id: Int,
    val groupName: String? = null,
)

@Destination<RootGraph>(navArgs = ProfileScreenNavArgs::class)
@Composable
fun ProfileScreen() {
    // 参数不进入 Composable，由 ViewModel 获取
}
```

在 ViewModel 中获取：

```kotlin
@HiltViewModel
class ProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    val navArgs = ProfileScreenDestination.argsFrom(savedStateHandle)
    // 或者使用生成的扩展
    val navArgs = savedStateHandle.navArgs<ProfileScreenNavArgs>()
}
```

#### 导航时传递参数

```kotlin
// 直接调用 Destination（参数在 invoke 函数中）
navigator.navigate(ProfileScreenDestination(id = 42, name = "John"))

// 或者构造 NavArgs
val args = ProfileScreenDestination.NavArgs(id = 42, name = "John")
navigator.navigate(ProfileScreenDestination(args))
```

#### 支持的参数类型

`String`, `Boolean`, `Int`, `Long`, `Float`, `Parcelable`, `java.io.Serializable`, `Enum`, `@kotlinx.serialization.Serializable` 注解类型。

自定义类型通过 `@NavTypeSerializer` 注册：

```kotlin
@NavTypeSerializer
class ColorTypeSerializer : DestinationsNavTypeSerializer<Color> {
    override fun toRouteString(value: Color): String =
        value.toArgb().toString()
    override fun fromRouteString(routeStr: String): Color =
        Color(routeStr.toInt())
}
```

### 1.6 导航图级别的参数

```kotlin
data class ProfileGraphArgs(val userId: String)

@NavGraph<RootGraph>(navArgs = ProfileGraphArgs::class)
annotation class ProfileGraph
```

获取：

```kotlin
// NavGraph 参数是可空的（因为可以导航到图内的某个 Destination 而不经过图入口）
val args: ProfileGraphArgs? = savedStateHandle.navGraphArgs<ProfileGraphArgs>()
val args: ProfileGraphArgs = savedStateHandle.requireNavGraphArgs()  // 确定有值时
```

### 1.7 测试时模拟参数传递

#### 模拟 SavedStateHandle

```kotlin
val savedStateHandle = ProfileScreenNavArgs(id = 1, name = "test")
    .toSavedStateHandle()
val vm = ProfileViewModel(savedStateHandle)
```

#### 模拟 DestinationsNavigator

```kotlin
val testNavController = TestNavHostController(LocalContext.current)
val navigator = TestDestinationsNavigator(testNavController)

// 在测试 Composable 中传入
composeTestRule.setContent {
    MyScreen(navigator = navigator)
}
```

#### 模拟完整导航环境

```kotlin
@RunWith(AndroidJUnit4::class)
class NavigationTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testNavigation() {
        composeTestRule.setContent {
            DestinationsNavHost(navGraph = NavGraphs.app)
        }
        // composeTestRule 进行点击、断言
    }
}
```

---

## 2. 多模块使用

### 2.1 核心概念

| 角色 | 说明 | 示例 |
|---|---|---|
| **Provider（提供方）** | feature 模块，暴露导航图 | `feature-bluetooth` |
| **Consumer（消费方）** | app 模块，聚合外部导航图 | `app` |

### 2.2 Provider 端配置

**Gradle**——feature 模块使用 `destinations` 模式：

```kotlin
// feature-module/build.gradle.kts
ksp {
    arg("compose-destinations.mode", "destinations")   // 只生成目的地，不生成独立 NavGraph
    arg("compose-destinations.moduleName", "bluetooth") // 可选的模块名（避免类名冲突）
}
dependencies {
    implementation(libs.compose.destinations)
    ksp(libs.compose.destinations.ksp)
}
```

> **`mode = "destinations"` 的作用**：只生成 `Destination` 类 + `ModuleRegistry` 元数据，不做 NavGraph 聚合。由消费方来组合。
> 如果 feature 模块内部自成完整导航流，可用 `mode = "navgraphs"`（即默认值）。

**暴露导航图**：

```kotlin
package com.example.feature.bluetooth

import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.ExternalModuleGraph
import com.ramcosta.composedestinations.annotation.NavGraph
import com.ramcosta.composedestinations.annotation.parameters.CodeGenVisibility

// 1. 声明外部模块导航图（注入 ExternalModuleGraph）
@NavGraph<ExternalModuleGraph>
annotation class FeatureBluetoothGraph

// 2. 目的地全部 internal —— 只暴露导航图，不暴露单个 Destination
@Destination<FeatureBluetoothGraph>(start = true, visibility = CodeGenVisibility.INTERNAL)
@Composable
internal fun BluetoothScanScreen() {
    BluetoothScan { }
}

@Destination<FeatureBluetoothGraph>(visibility = CodeGenVisibility.INTERNAL)
@Composable
internal fun BluetoothConnectScreen(mac: String) {
    BluetoothConnect { }
}
```

**Key Point**：
- `@NavGraph<ExternalModuleGraph>` 是特殊的导航图标注，告诉 KSP "我这个图不知道父图是谁，由消费方决定"。
- `visibility = CodeGenVisibility.INTERNAL` 让生成的 Destination 类也是 internal，避免从外部模块误用。
- 只向外暴露生成的 `FeatureBluetoothNavGraph` 类（public）。

**KSP 生成**（Provider 端）：

```
build/generated/ksp/debug/kotlin/
├── com/ramcosta/composedestinations/generated/
│   ├── NavGraphs.kt
│   ├── destinations/
│   │   ├── BluetoothScanScreenDestination.kt    (internal)
│   │   └── BluetoothConnectScreenDestination.kt (internal)
│   └── navgraphs/
│       └── FeatureBluetoothNavGraph.kt          (public)
└── _generated/.../moduleregistry/
    └── _ModuleRegistry_xxx.kt                   (元数据)
```

### 2.3 Consumer 端配置

**Gradle**——app 模块不需要特殊 KSP 模式（默认 `navgraphs`）：

```kotlin
// app/build.gradle.kts
dependencies {
    implementation(projects.featureBluetooth)  // 依赖 feature 模块
    implementation(libs.compose.destinations)
    ksp(libs.compose.destinations.ksp)
}
```

**导入外部导航图**：

```kotlin
package com.example.app

import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.ExternalNavGraph
import com.ramcosta.composedestinations.annotation.NavHostGraph
import com.ramcosta.composedestinations.generated.navgraphs.FeatureBluetoothNavGraph

// 1. 创建自定义 NavHostGraph
@NavHostGraph
annotation class AppGraph {
    // 2. 通过 @ExternalNavGraph 显式导入外部模块的导航图
    @ExternalNavGraph<FeatureBluetoothNavGraph>
    companion object Includes
}

// 3. 本地目的地使用 AppGraph
@Destination<AppGraph>(start = true)
@Composable
fun HomeScreen(navigator: DestinationsNavigator) {
    // navigator.navigate(FeatureBluetoothNavGraph)  // 导航到外部模块
}
```

**挂载**：

```kotlin
// 使用 AppGraph（含外部子图）代替 RootGraph
DestinationsNavHost(navGraph = NavGraphs.app)
```

**生成的导航结构**：

```
AppNavGraph
├── HomeScreen 🏁
├── AboutScreen
├── SettingsScreen
└── FeatureBluetoothNavGraph 🧩  ← 外部模块
      ├── BluetoothScanScreen 🏁
      └── BluetoothConnectScreen(mac: String)
```

> **关键点**：`ExternalModuleGraph` **不会自动发现和聚合**。消费方必须通过 `@ExternalNavGraph<>` 显式声明导入，否则外部导航图不会出现在 `NavGraphs` 中。

### 2.4 多模块嵌套规则总结

```
feature 模块                        app 模块
┌─────────────────────────┐       ┌──────────────────────────┐
│ ksp: mode = "destinations"│      │ ksp: mode = "navgraphs" (默认) │
│                           │       │                          │
│ @NavGraph<ExternalModuleGraph>│  │ @NavHostGraph             │
│ annotation class FGraph  │       │ annotation class AppGraph { │
│                           │       │   @ExternalNavGraph<FNavGraph>│
│ ← 生成 FNavGraph (public) │ ──→  │   companion object Includes│
│ ← 生成 _ModuleRegistry   │ 依赖  │ }                        │
└─────────────────────────┘       │ → 生成 AppNavGraph 含 FNavGraph│
                                  └──────────────────────────┘
```

### 2.5 跨模块导航跳转

#### 从 app 导航到 feature 的导航图

```kotlin
// 导航到外部模块的整个图（跳转到其 start 目的地）
navigator.navigate(FeatureBluetoothNavGraph)

// 如果外部导航图有 navArgs
navigator.navigate(FeatureBluetoothNavGraph(userId = "123"))
```

#### 跨模块导航到外部模块的特定 Destination

**不推荐**——因为 Destination 类在 feature 模块被标记为 `internal`。但如果需要：

```kotlin
// feature 模块必须将 Destination visibility 设为 PUBLIC
@Destination<FeatureBluetoothGraph>(visibility = CodeGenVisibility.PUBLIC)
@Composable
fun SomeScreen() { /*...*/ }

// app 模块直接在 NavHostGraph Includes 中导入 Destination
@NavHostGraph
annotation class AppGraph {
    @ExternalNavGraph<FeatureBluetoothNavGraph>
    @ExternalDestination<SomeScreenDestination>  // 额外导入特定目的地
    companion object Includes
}

// 然后可以直接导航
navigator.navigate(SomeScreenDestination)
```

> 更推荐的做法是：feature 模块暴露一个 NavGraph 入口 + 内部自行处理所有子导航，app 只需导航到 NavGraph 即可。

#### navigate 与 navigateToGraph 的语义差异

```kotlin
// navigator.navigate(NavGraph) → 导航到该图的 startRoute
navigator.navigate(FeatureBluetoothNavGraph)

// 如果 NavGraph 有 startRoute 且有必传参数，调用会要求传递
navigator.navigate(
    FeatureBluetoothNavGraph(startRouteArgs = ConnectScreenNavArgs(mac = "AA:BB"))
)
```

### 2.6 跨模块参数传递

#### Provider 方定义参数

```kotlin
// feature-module
data class BluetoothConnectNavArgs(val mac: String)

// 方式一：直接在 Composable 参数中（参数为 public）
@Destination<FeatureBluetoothGraph>(visibility = CodeGenVisibility.INTERNAL)
@Composable
internal fun BluetoothConnectScreen(mac: String) { /*...*/ }

// 方式二：navArgs 数据类（数据类必须 public）
@Destination<FeatureBluetoothGraph>(
    visibility = CodeGenVisibility.INTERNAL,
    navArgs = BluetoothConnectNavArgs::class
)
@Composable
internal fun BluetoothConnectScreen() { /*...*/ }
```

**关键**：如果要让 app 模块能传递参数导航到 feature 的某个 Destination，该 Destination 对应的 `Destination` 对象、NavArgs 类、以及参数类型都必须 **public** 或至少 `CodeGenVisibility.PUBLIC`。

#### Consumer 方传递参数

```kotlin
// app 模块
navigator.navigate(
    FeatureBluetoothNavGraph(
        // 如果有图级别的 navArgs
        graphArg = "...",
        // 如果 startRoute 有必传参数
        startRouteArgs = BluetoothConnectNavArgs(mac = "AA:BB:CC:DD:EE:FF")
    )
)
```

#### 跨模块返回结果

```kotlin
// Provider 方定义结果类型
data class BluetoothResult(val deviceName: String, val connected: Boolean)

// Provider 方标记结果类型
@Destination<FeatureBluetoothGraph>(
    visibility = CodeGenVisibility.INTERNAL,
    navArgs = ResultProfileScreenNavArgs::class
)
@Composable
internal fun ResultProfileScreen(
    resultBackNavigator: ResultBackNavigator<BluetoothResult>,
) {
    Button(onClick = {
        resultBackNavigator.setResult(BluetoothResult("Device1", true))
    }) { Text("确认") }
}

// Consumer 方接收结果
navigator.navigateForResult(
    FeatureBluetoothNavGraph(),
    onResult = { result: BluetoothResult ->
        // 处理返回结果
    }
)
```

---

## 3. 重要细节与常见坑

### 3.1 编译期校验机制

- **每个 NavGraph 必须有且仅有一个 `start = true` 的元素**（Destination 或嵌套 NavGraph）。编译期 KSP 强校验，多了或少了都会报错。
- **外部模块 NavGraph 的 start 可以在消费方覆盖**：

```kotlin
@ExternalNavGraph<FeatureBluetoothNavGraph>(start = true)  // 改为起点
```

### 3.2 `NavGraphs.xxx` 与直接使用生成类的区别

```kotlin
// 两者等价：
DestinationsNavHost(navGraph = NavGraphs.app)      // 通过 NavGraphs 对象
DestinationsNavHost(navGraph = AppNavGraph)         // 直接使用生成的 NavGraph
```

`NavGraphs` 对象会聚合模块内 **所有** 顶层 NavGraph，便于统一管理。

### 3.3 `mode = "destinations"` 的 KSP 配置

| `mode` 值 | 行为 | 适用场景 |
|---|---|---|
| `"navgraphs"` (默认) | 生成完整 NavGraph 结构，聚合所有目的地 | 独立 app 模块 |
| `"destinations"` | 只生成 Destination 类 + ModuleRegistry | feature 模块，供外部聚合 |

feature 模块用了 `"destinations"` 模式后：
- 生成的 `NavGraphs` 对象是 `internal`
- 但生成的 NavGraph 类（如 `FeatureBluetoothNavGraph`）是 `public`
- 消费方可以直接 import `FeatureBluetoothNavGraph` 来引用

### 3.4 `@ExternalNavGraph` vs `@NavGraph<ExternalModuleGraph>` 的区别

| 注解 | 位置 | 作用 |
|---|---|---|
| `@NavGraph<ExternalModuleGraph>` | Provider（feature 模块） | 声明"我不知道父图是谁" |
| `@ExternalNavGraph<XxxNavGraph>` | Consumer（app 模块）的 `Includes` | 显式导入外部导航图 |

两者必须配对使用，缺一不可。

### 3.5 修改目的地归属图时的操作清单

当需要把现有的 `@Destination<RootGraph>` 改为 `@Destination<AppGraph>` 时，需要改：

1. **Destination 注解**：每个 `@Destination` 的泛型参数
2. **import**：`RootGraph` → `AppGraph` + `ExternalNavGraph`
3. **DestinationsNavHost**：`NavGraphs.root` → `NavGraphs.app`
4. **代码中对 NavGraphs 的引用**：如动态构建目的地列表 `NavGraphs.app.destinations`
5. **Clean Build**：`./gradlew clean :app:assembleDebug` 确保 KSP 重新生成

### 3.6 修改不生效时的排查顺序

1. `./gradlew clean :feature:compileDebugKotlin :app:assembleDebug` —— 先编译 feature，再编译 app
2. 检查 KSP 生成目录 `build/generated/ksp/` 确认生成内容是否正确
3. 检查 `@ExternalNavGraph<>` 中的类型是否能解析（IDE 补全可用 = 编译期能发现）
4. 确认 feature 模块的 `_ModuleRegistry_xxx.kt` 中 `topLevelGraphs` 包含正确的 NavGraph 名称

### 3.7 Hilt + DestinationsNavigator 集成要点

```kotlin
// ViewModel 中不能注入 DestinationsNavigator（它不是 Hilt 可管理的依赖）
// 正确方式：通过 SavedStateHandle 获取参数
@HiltViewModel
class MyViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val id: Int = MyScreenDestination.argsFrom(savedStateHandle).id
}

// Composable 中直接作为参数接收
@Destination<AppGraph>
@Composable
fun MyScreen(
    navigator: DestinationsNavigator,  // 库自动注入
    vm: MyViewModel = hiltViewModel(),
) { /*...*/ }
```

### 3.8 `DestinationsNavigator` vs `NavController`

- 优先使用 `DestinationsNavigator`——它提供类型安全的导航 API
- `NavController` 只在需要其特有的 API（如 `popBackStack` 深层操作）时使用
- 从 `NavController` 获取 navigator：

```kotlin
val navigator = navController.toDestinationsNavigator()           // 非 Composable 中
val navigator = navController.rememberDestinationsNavigator()     // Composable 中
```

- 测试时使用 `EmptyDestinationsNavigator` 或 `TestDestinationsNavigator` 作为 previe 和测试的 mock

### 3.9 动态目的地列表的正确姿势

```kotlin
// home.kt 中的示例——从 NavGraphs 中动态构建菜单
private val homeItems: List<Direction> = NavGraphs.app.destinations
    .map { it as Direction }
    .filter { it !is HomeScreenDestination }  // 过滤掉自身

@Destination<AppGraph>(start = true)
@Composable
fun HomeScreen(navigator: DestinationsNavigator) {
    LazyColumn {
        items(homeItems, key = { it.route }) { item ->
            PrimaryButton(item.route.removeSuffix("_screen").uppercase()) {
                navigator.navigate(item)
            }
        }
    }
}
```

> **注意**：`homeItems` 是顶层 val，在类加载时初始化，可能发生在 KSP 生成类加载之前。如果列表为空，考虑改用 `remember { }` 或 `NavGraphs.app.destinations` 的懒求值版本。

### 3.10 生成导航结构可视化

```kotlin
ksp {
    arg("compose-destinations.mermaidGraph", "$rootDir/docs/navigation-graphs")
}
```

生成的 `.mmd` 文件可直接在 GitHub 上渲染为导航结构图，也可在 [mermaid.live](https://mermaid.live) 中查看。

---

## 附录：最小完整配置速查

### 单模块 app

```kotlin
// build.gradle.kts
plugins { alias(libs.plugins.google.ksp) }
dependencies {
    implementation(libs.compose.destinations)
    ksp(libs.compose.destinations.ksp)
}

// MainActivity.kt
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DestinationsNavHost(navGraph = NavGraphs.root)
        }
    }
}

// HomeScreen.kt
@Destination<RootGraph>(start = true)
@Composable
fun HomeScreen(navigator: DestinationsNavigator) { /*...*/ }
```

### 多模块（feature + app）

**Provider (feature-module)**:
```kotlin
// ksp { arg("compose-destinations.mode", "destinations") }

@NavGraph<ExternalModuleGraph>
annotation class FeatureGraph

@Destination<FeatureGraph>(start = true, visibility = CodeGenVisibility.INTERNAL)
@Composable
internal fun FeatureStartScreen() { /*...*/ }
```

**Consumer (app)**:
```kotlin
// dependencies { implementation(projects.featureModule) }

import com...generated.navgraphs.FeatureNavGraph

@NavHostGraph
annotation class AppGraph {
    @ExternalNavGraph<FeatureNavGraph>
    companion object Includes
}

DestinationsNavHost(navGraph = NavGraphs.app)
```
