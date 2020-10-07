@file:Suppress("LocalVariableName")

rootProject.name = "star-cruiser"

pluginManagement {
    val kotlin_version: String by settings
    plugins {
        kotlin("multiplatform") version kotlin_version
        kotlin("jvm") version kotlin_version
        kotlin("js") version kotlin_version
        kotlin("plugin.serialization") version kotlin_version
        id("com.github.johnrengelman.shadow") version "6.1.0"
        id("com.github.ben-manes.versions") version "0.33.0"
        id("org.jlleitschuh.gradle.ktlint") version "9.4.1"
        id("com.adarshr.test-logger") version "2.1.0"
    }
}

include(
    "shared",
    "server",
    "client"
)
