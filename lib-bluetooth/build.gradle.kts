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
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
