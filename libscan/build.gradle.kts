plugins {
    id("munch1182.commonbuild_lib")
    alias(libs.plugins.kotlin.android)
    id("munch1182.publish-local")
}
android {
    namespace = "com.munch1182.lib.scan"
}

publishToMavenLocal {
    groupId = "com.munch1182.android"
    artifactId = "scan"
    version = "0.1.0"
}

dependencies {
    debugImplementation(projects.lib)
    releaseApi(libs.munch1182.lib)

    implementation(libs.mlkit.barcode)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera2)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}