plugins {
    id("com.munch1182.android.testapp")
    id("com.munch1182.android.renameApk")
}

android {
    namespace = "com.munch1182.test.browser"
    buildFeatures { compose = true }
}

testApp {
    feature = "browser"
    label = "Test Browser"
}

renameApk {
    toDir = file("../apk")
}