plugins {
    id("com.munch1182.android.testapp")
    id("com.munch1182.android.renameApk")
}

android {
    namespace = "com.munch1182.test.audio"
    buildFeatures { compose = true }
}

testApp {
    feature = "audio"
    label = "Test Audio"
}

renameApk {
    toDir = file("../apk")
}