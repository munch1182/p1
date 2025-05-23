plugins {
    id("com.android.application")
    kotlin("android") // kotlinOptions
}

android {
    compileSdk = AppVersion.COMPILE_SDK

    defaultConfig {
        minSdk = AppVersion.MIN_SDK
        targetSdk = AppVersion.TARGET_SDK
        versionCode = AppVersion.versionCode()
        versionName = AppVersion.versionName()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }


    val signName = "release"
    // 配置文件名需要对齐
    val path = "gradle/key.properties"
    if (signingConfigs.findByName(signName) == null) {
        val file = rootProject.file(path)
        if (file.exists()) {
            signingConfigs.maybeCreate(signName).apply { sign(file, this) }
        }
    }


    buildTypes {
        release {
            if (signingConfig == null) {
                signingConfig = signingConfigs.findByName(signName)
            }
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
        }
        debug {
            if (signingConfig == null) {
                signingConfig = signingConfigs.findByName(signName)
            }
        }
    }


//    afterEvaluate {
//        tasks.named("assembleRelease") {
//            finalizedBy("renameApk")
//        }
//    }


    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}
//
//val renameApk by tasks.registering(Copy::class) {
//    val destDir = rootProject.file("apk")
//    val date = SimpleDateFormat("yyyyMMddHHmm").format(Date())
//    from("release/app-release.apk")
//    into(destDir)
//
//    val dc = android.defaultConfig
//    val name = "${dc.versionName}_${dc.versionCode}_${date}.apk"
//    println("name:${name}")
//    rename { name }
//}