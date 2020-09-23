@file:Suppress("PropertyName")

val kotlin_version: String by project
val kotlin_serialization_version: String by project

plugins {
    kotlin("js")
}

dependencies {
    implementation(project(":shared"))
}

kotlin {
    js {
        useCommonJs()
        browser()
    }
}
