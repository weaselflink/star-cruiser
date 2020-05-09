@file:Suppress("PropertyName")

val kotlin_version: String by project

plugins {
    id("org.jetbrains.kotlin.js") version "1.3.72"
}

group = "de.bissell.starcruiser"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-js"))
}

kotlin {
    target {
        useCommonJs()
        browser()
    }
}
