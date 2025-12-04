# lib上传

### 步骤

1. 添加`maven-publish`依赖

```kts
plugins {
    ///...
    id("maven-publish")
}
```

2. 添加`publishing`配置

```kts
android {

}
afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])

                groupId = "com.munch1182"
                artifactId = "artifactId"
                version = "1.0.0"
            }
        }
        // 发布到本地路径
        repositories {
            maven {
                url = uri("${rootProject.projectDir}/maven-local")
            }
        }
    }
}
```

3. 本地依赖

```kts
dependencyResolutionManagement {
    repositories {
        // ...
        maven { url uri ("file:///D:/my-local-maven") }
    }
}
```

4. 添加源代码文件

```kts
afterEvaluate {
    val sourcesJar = target.tasks.register("sourcesJar", Jar::class.java) {
        archiveClassifier.set("sources")
        val android = target.extensions.getByName("android") as LibraryExtension
        from(android.sourceSets.getByName("main").java.srcDirs)
    }
    publishing {
        publications {
            create<MavenPublication>("release") {
                //...
                artifact(sourcesJar)
            }
        }
    }

}

artifact(sourcesJar)
```

### 将逻辑写成完整的插件

[插件](./build-logic/src/main/kotlin/PublishToLocalPlugin.kt)

注意：kt类型的二进制插件需要注册，且可以直接在`build.gradle.kts`在注册(.gradle.kts脚本则可以直接通过文件名引用)