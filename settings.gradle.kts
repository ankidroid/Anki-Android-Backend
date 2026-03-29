dependencyResolutionManagement {
    repositories {
        maven {
            url = uri("${rootProject.projectDir}/build/localMaven")
        }
    }
}

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "Anki-Android-Backend"

include(":rsdroid-testing", ":rsdroid-instrumented", ":rsdroid")