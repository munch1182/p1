plugins {
    id("munch1182.commonbuild_lib")
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.munch1182.lib.widget"
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(projects.lib)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}