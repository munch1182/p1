plugins {
    id("munch1182.commonbuild_lib")
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.munch1182.libCamera"
}

dependencies {
    val camerax_version = "1.5.0-alpha03"
    implementation(projects.lib)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)

    implementation("androidx.camera:camera-lifecycle:${camerax_version}")
    implementation("androidx.camera:camera-view:${camerax_version}")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}