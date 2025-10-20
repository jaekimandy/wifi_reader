pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        // Try adding potential Zetic repository
        maven { url = uri("https://maven.pkg.github.com/zetic-ai/zetic-mlange") }
    }
}

rootProject.name = "WiFi Reader"
include(":app")
// Removed OpenCV module - not being used