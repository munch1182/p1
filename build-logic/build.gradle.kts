plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

// 插件注册
gradlePlugin {
    plugins {
        create("publish-local") {
            id = "munch1182.publish-local" // 引用id
            implementationClass = "PublishToLocalPlugin"
        }
    }
}

dependencies {
    implementation(libs.kotlin.gradlePlugin)
    implementation(libs.gradle)
}