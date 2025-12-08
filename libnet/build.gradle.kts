plugins {
    id("munch1182.commonbuild_lib")
    alias(libs.plugins.kotlin.android)
    id("munch1182.publish-local")
}
android {
    namespace = "com.munch1182.lib.net"
}

publishToMavenLocal {
    groupId = "com.munch1182.android"
    artifactId = "net"
    version = "0.1.0"
}

dependencies {
    debugImplementation(projects.lib)
    releaseApi(libs.munch1182.lib)

    implementation(platform(libs.okhttp.bom))
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.gson)

    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}