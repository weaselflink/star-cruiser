@file:Suppress("PropertyName")

val kotlin_version: String by project

plugins {
    kotlin("js")
    kotlin("plugin.serialization")
}

group = "de.bissell.starcruiser"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
}

dependencies {

    implementation(project(":shared"))

    implementation(kotlin("stdlib-js"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-js:0.20.0")
    implementation("org.jetbrains:kotlin-react:16.13.1-pre.104-kotlin-1.3.72")
    implementation("org.jetbrains:kotlin-react-dom:16.13.1-pre.104-kotlin-1.3.72")
    implementation(npm("react", "16.13.1"))
    implementation(npm("react-dom", "16.13.1"))
}

kotlin {
    target {
        useCommonJs()
        browser()
    }
}
