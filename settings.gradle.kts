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
        id("com.adarshr.test-logger") version "3.2.0"
        id("com.github.johnrengelman.shadow") version "8.1.1"
        id("com.github.ben-manes.versions") version "0.51.0"
        id("se.ascp.gradle.gradle-versions-filter") version "0.1.16"
        id("org.jlleitschuh.gradle.ktlint") version "10.3.0"
    }
}

include(
    "shared",
    "server",
    "client"
)
