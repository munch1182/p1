plugins {
    id("munch1182.commonbuild_lib")
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.munch1182.libview"
}

dependencies {

    implementation(projects.lib)
    implementation(libs.androidx.core.ktx)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}