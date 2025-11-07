# 编译相关

### 一键编译多种包

```kotlin

// 创建一个任务来构建所有要发布的版本到指定文件夹
tasks.register("buildPublishFlavors") {
    group = "build"
    description = "Build specified product flavors in release mode"

    // 定义要构建的flavors
    val flavors = listOf("zkeg" to "apk", "core" to "aab")
    build2Dir(flavors, "build/apk")
}

```

执行该任务即可将所有传入的flavors生成并复制到指定文件夹