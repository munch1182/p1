# 启动优化

### 1. 计时指令

```shell
adb shell am start -S -W com.munch1182.p1/.MainActivity
```

结果输出：

```shell
Stopping: com.munch1182.p1
Starting: Intent { act=android.intent.action.MAIN cat=[android.intent.category.LAUNCHER] cmp=com.munch1182.p1/.SplashActivity }
Status: ok
LaunchState: COLD
Activity: com.munch1182.p1/.MainActivity
TotalTime: 1828
WaitTime: 1835
Complete
```

该指令同时会在`logcat`中输出时间，使用`Displayed`过滤即可看见计时。

### 2. Application 优化

1. 只将必要的模块放在`Applicaiton`主线程中。
2. 组件需要在主线程中初始化的，可以**延迟初始化**。
3. 组件不需要在主线程初始化的，可以放在**子线程中初始化**。
4. 使用更优秀的组件来降低启动耗时，比如替换掉`SharedPreferences`。

### 3. 显示优化

1. 冷启动时系统会添加一个`type`为`TYPE_APPLICATION_STARTING`的启动窗口`Starting Window`，在初始化完成后会移除此窗口并显示应用启动界面，其主题会与`LAUNCHER Activity`/`Application theme`一致。
   如果没有设置则用系统默认的主题，系统会将屏幕填充主题默认的背景色，亮系主题填充白色，暗系主题填充黑色，此即启动时的黑/白屏，效果为点击`app`后，屏幕白屏或者黑屏，然后显示`app`界面，显示效果不好。
2. 给启动页设置主题可优化视觉显示，即冷启动时显示启动页样式，初始化完成后显示启动页实现无缝过渡。
   而在`Android 12`后，新增了[SplashScreen API](https://developer.android.google.cn/develop/ui/views/launch/splash-screen?hl=zh-cn)用于优化了启动时的显示(且不建议自定义`Splash`页面)，而且提供对应兼容库[兼容](https://developer.android.google.cn/develop/ui/views/launch/splash-screen/migrate?hl=zh-cn)
   `API 23`之后的设备。
3. 将显示图标设置为动画效果更好。

### ~~4. 闪屏页优化~~

~~1. 如果闪屏页有显示**固定时间**，可以将组件加载时间算在此显示时间内，即：**闪屏页展示时间 = 展示总时间 - 组件初始化时间**，来同步不同配置的闪屏时间。~~

2. 组件初始化流程:

```
Application: initialized -> attachBaseContext() -> onCreate() ->
Activity: attachBaseContext() -> onCreate() -> onStart() -> onResume() -> onWindowFocusChanged()
//onWindowFocusChanged()的回调表示View的流程绘制完毕用户真正可操作
//如果该页面可操作之前即被跳转，则onWindowFocusChanged()不会回调
```

组件初始化时间即可视为`Application.attachBaseContext()`到`Activity.onWindowFocusChanged()`之间的用时。

### 5. 总结

延迟非必要的初始化，异步耗时的初始化，优化启动页的显示。