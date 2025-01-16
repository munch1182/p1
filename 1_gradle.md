# 对 gradle 的修改

修改目标：

1. 统一版本和依赖
2. 统一`build.gradle`文件共有内容
3. debug 版本 app 版本号自增; 正式版根据 tag 自动更改版本号(可选)

前两点主要是针对多模块，如果有统一的库处理或者模板生成，也可以用于项目生成的统一。第三点则是用于日常使用，特别是小 bug 修复后交予测试时的情形，比时间作为版本号更新精细。

## 统一版本和依赖

当前版本`AS(2024.2.2)`已经默认选中采用`kts`并使用`libs.versions.toml`来统一版本和依赖。
此方法相较于之前版本的几种方法，支持新版本提示，支持跳转，支持代码提示，支持更改同步，总的来说还是很完美的，所以建议直接使用该方法。
注意`gradle/libs.versions.toml`是[约定](https://docs.gradle.org/current/userguide/version_catalogs.html)的默认的位置和名称。

## 统一`build.gradle`文件内容

指的是将`app/build.gradle`中的共用内容提取成文件，然后交由所有模块的`build.gradle`调用，且模块可以省略/覆盖对应设置。
对于`groovy`脚本，可以直接提取成文件并使用`apply from: /path/common.gradle`使用即可。
但对于`kts`脚本，虽然可以使用`apply(from = rootProject.file("/path/common.gradle.kts"))`来引用文件，但因为[类型安全的原因](https://docs.gradle.org/current/userguide/kotlin_dsl.html#type-safe-accessors)无法这样简单使用。
`kts`脚本可以使用[precompiled script plugins](https://docs.gradle.org/current/userguide/custom_plugins.html#sec:precompiled_plugins)的方式来达到此效果。

步骤： [使用插件统一文件内容](./1_1_plugin.md)

相较于`gradle`脚本，`kts`脚本初次编写更麻烦，使用更麻烦，但因为有完整的代码提示与跳转，后续添加更改理解会更简单。

## 自增版本号

当提交后，无需任何更改，版本号即会自增。

与`groovy`相同，`kts`脚本也可以直接在脚本中调用`kotlin`方法，所以该实现很简单，在方法中实现对 git 相关参数的获取，并将其赋值给版本号即可。

将`git`的提交次数作为版本号，将最近的`tag`作为版本名即为最简单的实现。
该实现的限制是：所有交付的版本必须先提交，所有发布的版本必须要有 tag。

这部分的内容并不复杂，完全可以根据需要自行更改。

## 其它部分

### 更改输入 apk 名字

### 自动加载签名配置文件
