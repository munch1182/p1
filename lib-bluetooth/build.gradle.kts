plugins {
    id("com.munch1182.android.commonbuild_lib")
}

android {
    namespace = "com.munch1182.lib.bluetooth"
}


dependencies {
    compileOnly(libs.androidx.annotation.jvm)
    implementation(projects.libCommon)
    implementation(projects.libAndroid)
    implementation(libs.androidx.startup)
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.androidx.startup)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
