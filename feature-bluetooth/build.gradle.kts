plugins {
    id("com.munch1182.android.commonbuild_lib")
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.ksp)
    alias(libs.plugins.google.hilt)
}

android {
    namespace = "com.munch1182.feature.bluetooth"
    buildFeatures {
        compose = true
    }
}

ksp {
    arg("compose-destinations.mode", "destinations")   // 只生成目的地，不生成独立 NavGraph
    arg("compose-destinations.moduleName", "bluetooth") // 可选的模块名（避免类名冲突）
}

dependencies {
    implementation(projects.libCommon)
    implementation(projects.libAndroid)
    implementation(projects.libBluetooth)
    implementation(projects.coreUi)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)

    implementation(libs.compose.destinations)
    ksp(libs.compose.destinations.ksp)

    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)

    implementation(libs.hilt.android)
    ksp(libs.hilt.ksp)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.kotlinx.coroutines.core)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
