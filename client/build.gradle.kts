@file:Suppress("PropertyName")

val kotlin_version: String by project

plugins {
    kotlin("js")
    kotlin("plugin.serialization")
}

repositories {
    mavenCentral()
    maven(url = "https://jitpack.io")
}

dependencies {

    implementation(project(":shared"))

    implementation(kotlin("stdlib-js"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-js:0.20.0")
}

kotlin {
    target {
        useCommonJs()
        browser()
    }
}
