plugins {
    id("com.munch1182.android.commonbuild_app")
}

android {
    namespace = "com.munch1182.p1"
}
dependencies {
    implementation(projects.coreCommon)
    implementation(projects.coreAndroid)

    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)

    debugImplementation(libs.leakcanary.android)

    implementation(libs.crashreport)
}
