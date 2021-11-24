@file:Suppress("PropertyName", "SuspiciousCollectionReassignment")

import org.jetbrains.kotlin.gradle.dsl.KotlinCompile

val kotlin_version: String by project
val kotlin_serialization_version: String by project

plugins {
    kotlin("js")
}

dependencies {
    implementation(projects.shared)
}

kotlin {
    js {
        useCommonJs()
        browser()
    }
}

tasks {
    withType<KotlinCompile<*>>().all {
        kotlinOptions {
            freeCompilerArgs += listOf("-Xopt-in=kotlin.RequiresOptIn")
        }
    }
}
