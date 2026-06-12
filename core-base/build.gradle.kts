plugins {
    id("com.munch1182.android.commonbuild_lib")
    alias(libs.plugins.google.ksp)
    alias(libs.plugins.google.hilt)
}

android {
    namespace = "com.munch1182.core.base"
}

dependencies {
    implementation(projects.libCommon)
    implementation(projects.libAndroid)

    implementation(libs.androidx.appcompat)

    implementation(libs.hilt.android)
    ksp(libs.hilt.ksp)
}
