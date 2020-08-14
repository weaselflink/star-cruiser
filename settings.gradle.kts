@file:Suppress("LocalVariableName")

rootProject.name = "star-cruiser"

pluginManagement {
    val kotlin_version: String by settings
    plugins {
        kotlin("multiplatform") version kotlin_version
        kotlin("jvm") version kotlin_version
        kotlin("js") version kotlin_version
        kotlin("plugin.serialization") version kotlin_version
        id("com.github.johnrengelman.shadow") version "6.0.0"
        id("com.github.ben-manes.versions") version "0.29.0"
        id("org.jlleitschuh.gradle.ktlint") version "9.3.0"
    }
}

include(
    "shared",
    "server",
    "client"
)
