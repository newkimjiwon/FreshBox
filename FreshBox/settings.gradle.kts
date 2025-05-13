pluginManagement {
    repositories {
        gradlePluginPortal()  // ✅ KSP 플러그인용
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "FreshBox"
include(":app")