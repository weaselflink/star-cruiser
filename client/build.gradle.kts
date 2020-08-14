@file:Suppress("PropertyName")

val kotlin_version: String by project
val kotlin_serialization_version: String by project

plugins {
    kotlin("js")
}

dependencies {

    implementation(project(":shared"))

    implementation(kotlin("stdlib-js"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-js:$kotlin_serialization_version")
}

kotlin {
    target {
        useCommonJs()
        browser()
    }
}
