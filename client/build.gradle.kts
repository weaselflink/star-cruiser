@file:Suppress("PropertyName", "SuspiciousCollectionReassignment")

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

tasks {
    withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>>().all {
        kotlinOptions {
            freeCompilerArgs += listOf("-Xopt-in=kotlin.RequiresOptIn")
        }
    }
}
