plugins {
    id("munch1182.commonbuild_lib")
    alias(libs.plugins.kotlin.android)
    id("munch1182.publish-local")
}
android {
    namespace = "com.munch1182.lib.bluetooth"
}

publishToMavenLocal {
    groupId = "com.munch1182.android"
    artifactId = "bluetooth"
    version = "0.1.0"
}

dependencies {
    debugImplementation(projects.lib)
    releaseImplementation(libs.munch1182.lib)

    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}