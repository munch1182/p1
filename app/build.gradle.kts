plugins {
    id("com.munch1182.android.commonbuild_app")
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.jetbrains.kotlin.serialization)
    alias(libs.plugins.google.ksp)
    alias(libs.plugins.google.hilt)
}

android {
    namespace = "com.munch1182.p1"
    buildFeatures {
        compose = true
    }
}
dependencies {
    implementation(projects.coreCommon)
    implementation(projects.coreAndroid)

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity)

    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.material)

    implementation(libs.androidx.compose.material.icons)

    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.navigation3.runtime)

    implementation(libs.compose.destinations)
    ksp(libs.compose.destinations.ksp)

    implementation(libs.crashreport)

    implementation(libs.hilt.android)
    ksp(libs.hilt.ksp)
    implementation(libs.hilt.navigation.compose)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    debugImplementation(libs.leakcanary.android)

    debugImplementation(libs.junit)
}
