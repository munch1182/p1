plugins {
    id("munch1182.commonbuild_lib")
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.munch1182.lib.scan"
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    //noinspection UseTomlInstead
    /*implementation("com.quickbirdstudios:opencv-contrib:4.5.3.0")*/
    implementation(projects.lib)

    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    api(libs.androidx.camera.view)
    implementation(libs.androidx.camera.mlkit.vision)

    implementation(libs.barcode.scanning)
}