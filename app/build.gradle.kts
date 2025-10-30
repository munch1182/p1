plugins {
    id("munch1182.commonbuild_app")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.jetbrains.kotlin.serialization)
}

android {
    namespace = "com.munch1182.p1"

    buildFeatures {
        compose = true
        viewBinding = true
    }

    setAPkRename()

    flavorDimensions += "blue"
    productFlavors {
        create("zkeg") {
            dimension = "blue"
            applicationIdSuffix = ".zkeg"
            versionNameSuffix = "-zkeg"
        }
        create("core") {
            dimension = "blue"
        }
    }
}

dependencies {
    implementation(projects.lib)
    implementation(projects.libweight)
    implementation(projects.libscan)
    implementation(projects.libbluetooth)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    // https://developer.android.google.cn/guide/navigation/navigation-3/get-started?hl=zh_cn
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.kotlinx.serialization.core)
    // https://developer.android.google.cn/develop/ui/compose/libraries?hl=zh-cn#hilt
    // https://developer.android.google.cn/training/dependency-injection/hilt-android?hl=zh-cn
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.ksp)

    implementation(libs.androidx.material3)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.compose.runtime.livedata)

    implementation(libs.androidx.core.splash)
    implementation(libs.androidx.constraintlayout)

    implementation(platform(libs.okhttp.bom))
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)

    // https://github.com/GetStream/webrtc-android
    //noinspection UseTomlInstead
    implementation("io.getstream:stream-webrtc-android:1.3.10")

    implementation(libs.androidx.camera.view)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

}