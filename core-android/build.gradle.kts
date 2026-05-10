plugins {
    id("com.munch1182.android.commonbuild_lib")
}

android {
    namespace = "com.munch1182.core.android"
}

dependencies {

    implementation(projects.coreCommon)

    implementation(libs.androidx.startup)
    implementation(projects.libXlog)
    implementation(libs.mmkv)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
