plugins {
    id("com.munch1182.android.testapp")
    id("com.munch1182.android.renameApk")
}

android {
    namespace = "com.munch1182.test.net"
    buildFeatures { compose = true }

    packaging {
        resources {
            excludes.addAll(listOf("META-INF/INDEX.LIST", "META-INF/io.netty.versions.properties"))
        }
    }
}

testApp {
    feature = "net"
    label = "Test Net"
}

renameApk {
    toDir = file("../apk")
}