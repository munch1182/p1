# app页面结构

可使用一些几种组织结构：

1. 原生`Activity`+ `Activity`+`Fragment`+`Fragment`的方式
2. 使用单`Activity`+若干`Fragment`的方式
3. 使用单`Activity`+若干`Compose`的方式

使用2、3可以使用官方库[navigation](https://developer.android.google.cn/guide/navigation/navigation-3/get-started?hl=zh_cn)来管理页面跳转。

### 使用单`Activity` + 若干`Compose` + `navigation-3`

1. 添加依赖:

```gradle
implementation(libs.androidx.navigation3.runtime)
implementation(libs.androidx.navigation3.ui)
```

2. 创建一个`Compose`函数并在其中创建[NavHostController](./main/java/com/munch1182/p1/views.kt)并定义导航结构；参考`AppView`(`com.munch1182.p1.ViewsKt.AppView`)以及[官方示例](https://developer.android.google.cn/guide/navigation/navigation-3/basics?hl=zh_cn)
3. 使用`Compose`函数作为显示单位(页面)，使用`NavHostController`控制切面切换
4. 在`Activity`中显示带有`NavHostController`的`Compose`函数