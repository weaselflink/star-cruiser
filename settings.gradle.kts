@file:Suppress("LocalVariableName")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "star-cruiser"

pluginManagement {
    val kotlin_version: String by settings
    plugins {
        kotlin("multiplatform") version kotlin_version
        kotlin("jvm") version kotlin_version
        kotlin("js") version kotlin_version
        kotlin("plugin.serialization") version kotlin_version
        id("com.github.johnrengelman.shadow") version "7.1.0"
        id("com.github.ben-manes.versions") version "0.39.0"
        id("org.jlleitschuh.gradle.ktlint") version "10.2.0"
        id("com.adarshr.test-logger") version "3.1.0"
    }
}

include(
    "shared",
    "server",
    "client"
)
