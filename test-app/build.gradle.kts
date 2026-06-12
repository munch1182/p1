plugins {
    id("com.munch1182.android.commonbuild_app")
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.ksp)
    alias(libs.plugins.google.hilt)
}

android {
    namespace = "com.munch1182.test"
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(projects.libCommon)
    implementation(projects.libAndroid)
    implementation(projects.coreBase)
    implementation(projects.libBluetooth)
    implementation(projects.coreUi)
    implementation(projects.featureBluetooth)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)

    implementation(libs.hilt.android)
    ksp(libs.hilt.ksp)
    implementation(libs.hilt.navigation.compose)
}
