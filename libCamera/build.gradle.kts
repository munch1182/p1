plugins {
    id("munch1182.commonbuild_lib")
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.munch1182.libCamera"
}

dependencies {

    implementation(projects.lib)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)

    val camerax_version = "1.5.0-alpha03"
    implementation("androidx.camera:camera-camera2:${camerax_version}")
    implementation("androidx.camera:camera-lifecycle:${camerax_version}")
    implementation("androidx.camera:camera-view:${camerax_version}")
    implementation("com.google.zxing:android-core:3.3.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}