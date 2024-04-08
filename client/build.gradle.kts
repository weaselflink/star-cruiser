@file:Suppress("PropertyName", "SuspiciousCollectionReassignment")

import org.jetbrains.kotlin.gradle.dsl.KotlinCompile

val java_version: String by project
val kotlin_version: String by project
val kotlin_serialization_version: String by project

plugins {
    kotlin("multiplatform")
}

kotlin {
    js(IR) {
        browser()
        binaries.executable()
    }
}

dependencies {
    commonMainImplementation(projects.shared)
}

tasks {
    withType<KotlinCompile<*>>().all {
        kotlinOptions {
            freeCompilerArgs += listOf("-opt-in=kotlin.RequiresOptIn")
        }
    }
}
