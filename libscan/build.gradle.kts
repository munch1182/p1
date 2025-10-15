plugins {
    id("munch1182.commonbuild_lib")
    alias(libs.plugins.kotlin.android)
}
android {
    namespace = "com.munch1182.lib.scan"
}

dependencies {
    implementation(projects.lib)
    implementation(libs.mlkit.barcode)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera2)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}