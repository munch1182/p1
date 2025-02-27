pluginManagement {
    includeBuild("build-logic")
    repositories {
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
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}


rootProject.name = "p1"
include(":app")
include(":lib")
include(":libFloatWindow")
include(":libView")

// 能够以代码的方式引入module
// 示例：
// implementation(project(":lib))
// implementation(projects.lib)
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
