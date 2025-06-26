plugins {
    id("munch1182.commonbuild_app")

    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.munch1182.p1"

    buildFeatures {
        compose = true
        viewBinding = true
    }
    setAPkRename()

    flavorDimensions += "version"

    productFlavors {
        // View/Tool Windows/Build Variants
        create("dev") {
            versionNameSuffix = "_dev"
            applicationIdSuffix = ".dev"
            dimension = "version"
            signingConfig = signingConfigs.findByName("release")
        }

        create("publish") {
            dimension = "version"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)

    implementation(libs.androidx.core.splash)

    implementation(projects.lib)
    implementation(projects.libwidget)

    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.compose.runtime.livedata)

    implementation(libs.gson)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation("com.github.lincollincol:PCM-Decoder:1.0")

    implementation("androidx.media3:media3-exoplayer:1.6.0")
    implementation("androidx.media3:media3-ui:1.6.0")
    implementation("androidx.media3:media3-session:1.6.0")

    implementation("com.quickbirdstudios:opencv-contrib:4.5.3.0")
}