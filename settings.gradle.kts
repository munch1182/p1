pluginManagement {
    includeBuild("build-logic")
    includeBuild("test-app-plugin")
    repositories {
        maven { url = uri("https://mirrors.tencent.com/nexus/repository/maven-public/") }
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    @Suppress("UnstableApiUsage")
    repositories {
        maven { url = uri("https://mirrors.tencent.com/nexus/repository/maven-public/") }
        google()
        mavenCentral()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "P1"
include(":app")
include(":lib-common")
include(":lib-android")
include(":lib-xlog")
include(":lib-bluetooth")
include(":core-router")
include(":core-ui")
include(":core-base")
include(":feature-bluetooth")
include(":test-app-bluetooth")
include(":feature-audio")
include(":test-app-audio")
include(":feature-browser")
include(":test-app-browser")
include(":feature-net")
include(":test-app-net")
