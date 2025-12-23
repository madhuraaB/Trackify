// This block defines where Gradle looks for plugins themselves.
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

// This block defines where Gradle looks for dependencies (like MPAndroidChart).
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // FIX: The essential repository for MPAndroidChart
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "Trackify"
include(":app")
