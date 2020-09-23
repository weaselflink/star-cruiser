@file:Suppress("PropertyName")

val kotlin_version: String by project
val kotlin_serialization_version: String by project

plugins {
    kotlin("js")
}

dependencies {

    implementation(project(":shared"))

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlin_serialization_version")
}

kotlin {
    js {
        useCommonJs()
        browser()
    }
}
