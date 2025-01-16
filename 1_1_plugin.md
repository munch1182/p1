# 使用插件统一文件内容

1. 新建插件模块
   方法 a： 新建`buildSrc`文件夹。该文件夹是`gradle`预留模块，`gradle`会自动编译并将其中内容加载到其他模块中;
   方法 b： 新建文件夹，并在`settings.gradle.kts`中手动引入:
   ```kts
    pluginManagement{
        includeBuild("build-logic") // 模块名为build-logic
    }
   ```
2. 创建插件结构:
   ```
   build-logic
   ├── src
   │   └── main
   │       └── kotlin
   └── build.gradle.kts
   └── settings.gradle.kts
   ```
3. 修改[settings.gradle.kts](./build-logic/settings.gradle.kts)让`build-logic`模块使用`libs`
4. 修改[build.gradle.kts](./build-logic/build.gradle.kts)引入依赖
5. 创建[commonbuild_app.gradle.kts](./build-logic/src/main/kotlin/munch1182.commonbuild_app.gradle.kts)文件来统一`android application`的内容。（此方法需要对`application`和`library`分别处理）
6. 在[模块](./app/build.gradle.kts)中引入插件删除重复内容，引入插件，并保留差异内容
   引入插件： 使用`id(plugin_file_name)`的方式引入

- 在编写共同内容时可能会报错，但编译完成后如同正常的 kts 脚本，有代码提示与跳转。
