# 对 gradle 的修改

修改目标：

1. 使用代理
2. 统一版本和依赖
3. 统一`build.gradle`文件共有内容
4. 一些自动化处理，包括 debug 版本号自增; 签名文件配置; 生成文件名修改等

## 使用代理

虽然大部分情况下可以直接使用网络下载`gradle`和相关库，但大多数情况下网络较慢，偶尔还会有网络不稳定的情况，所以建议使用代理。

1. 使用系统代理
2. 使用国内镜像

使用镜像需要进行配置，而且不能兼顾所有下载，所以建议使用系统代理。但如果要配置国内镜像，方法如下：

gradle 镜像

```properties
# gradle/wrapper/gradle-wrapper.properties
distributionUrl=https\://services.gradle.org/distributions/gradle-8.10.2-bin.zip
# 改为
distributionUrl=https\://mirrors.cloud.tencent.com/gradle/gradle-8.10.2-bin.zip
# 而且建议将-bin改为-all, 可以将源码一同下载
```

库代理

```gradle
<!-- settings.gradle.kts -->
pluginManagement {
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/jcenter") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://mavem.aliyun.com/repository/gradle-plugin")}
        maven { url = uri("https://mavem.aliyun.com/repository/public")}
    }
}
dependencyResolutionManagement{
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/jcenter") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://mavem.aliyun.com/repository/gradle-plugin")}
        maven { url = uri("https://mavem.aliyun.com/repository/public")}
    }
}
```

## 统一版本和依赖

当前版本`AS(2024.2.2)`已经默认选中采用`kts`并使用`libs.versions.toml`来统一版本和依赖。
此方法相较于之前版本的几种方法，支持新版本提示，支持跳转，支持代码提示，支持更改同步，除了会拖慢构建速度外总的来说还是很完美的，所以建议直接使用该方法。
注意`gradle/libs.versions.toml`是[约定](https://docs.gradle.org/current/userguide/version_catalogs.html)的默认的位置和名称。

## 统一`build.gradle`文件内容

指的是将`app/build.gradle`中的共用内容提取成文件，然后交由所有模块的`build.gradle`调用，且模块可以省略/覆盖对应设置。
对于`groovy`脚本，可以直接提取成文件并使用`apply from: /path/common.gradle`使用即可。
但对于`kts`脚本，虽然可以使用`apply(from = rootProject.file("/path/common.gradle.kts"))`来引用文件，但因为[类型安全的原因](https://docs.gradle.org/current/userguide/kotlin_dsl.html#type-safe-accessors)无法这样简单使用。
`kts`脚本可以使用[precompiled script plugins](https://docs.gradle.org/current/userguide/custom_plugins.html#sec:precompiled_plugins)的方式来达到此效果。

步骤： [使用插件统一文件内容](./1_1_plugin.md)

相较于`gradle`脚本，`kts`脚本初次编写更麻烦，使用更麻烦，但因为有完整的代码提示与跳转，后续添加更改理解会更简单。

## 一些自动化处理

### 自增版本号

当提交后，无需任何更改，版本号即会自增。

与`groovy`相同，`kts`脚本(`gradle.kts`)也可以直接在脚本中调用`kotlin`方法，所以该实现很简单，在方法中实现对 git 相关参数的获取，并将其赋值给版本号即可。

将`git`的tag的前两位作为版本号的前两位，将`git`的tag至今的提交次数作为版本号的第三位。

### 更改输入 apk 名字

[实现可见 RenameApk.kt](./build-logic/src/main/kotlin/Rename.kt)，该方法有局限，但实现较为简单。

### 自动加载签名配置文件

约定一个配置文件，在`build.gradle`读取该配置并赋值给签名配置即可。

注意：如果该文件在本地项目目录中，需要在`git`中忽略该文件，防止泄露。