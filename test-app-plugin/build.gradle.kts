plugins {
    `kotlin-dsl`
}
repositories {
    google()
    mavenCentral()
}
dependencies {
    implementation(libs.gradle)
}

gradlePlugin {
    plugins {
        create("testapp") {
            id = "com.munch1182.android.testapp"
            implementationClass = "TestAppPlugin"
        }
    }
}
