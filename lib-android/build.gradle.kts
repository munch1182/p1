plugins {
    id("com.munch1182.android.commonbuild_lib")
}

android {
    namespace = "com.munch1182.lib.android"
}

dependencies {

    implementation(projects.libCommon)

    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.androidx.startup)
    implementation(projects.libXlog)
    implementation(libs.androidx.datastore)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
