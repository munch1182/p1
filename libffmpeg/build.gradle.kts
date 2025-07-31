plugins {
    id("munch1182.commonbuild_lib")
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.munch1182.lib.ffmpeg"
}

dependencies {

    implementation(projects.lib)
    implementation("com.bihe0832.android:lib-ffmpeg-mobile-aaf:6.9.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}