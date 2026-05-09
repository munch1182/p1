plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
    maven { url = uri("https://mirrors.tencent.com/nexus/repository/maven-public/") }
}

dependencies {
    implementation(libs.gradle)
}

gradlePlugin {
    plugins {
        create("renameApk") {
            id = "com.munch1182.android.rename-apk"
            implementationClass = "ApkCopyRenamePlugin"
        }
    }
}