@file:Suppress("PropertyName", "SuspiciousCollectionReassignment")

import org.jetbrains.kotlin.gradle.dsl.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompile

val java_version: String by project
val kotlin_version: String by project
val ktor_version: String by project
val kotlin_serialization_version: String by project
val junit_version: String by project
val strikt_version: String by project

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlin_serialization_version")
            }
        }
    }

    jvm {
        withJava()

        compilations.all {
            kotlinOptions {
                jvmTarget = java_version
            }
        }

        compilations["main"].defaultSourceSet {
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
            }
        }

        compilations["test"].defaultSourceSet {
            dependencies {
                implementation("org.junit.jupiter:junit-jupiter:$junit_version")
                implementation("io.strikt:strikt-core:$strikt_version")
            }
        }
    }

    js {
        browser()
    }
}

tasks {
    withType<KotlinCompile<*>>().all {
        kotlinOptions {
            freeCompilerArgs += listOf("-Xopt-in=kotlin.ExperimentalUnsignedTypes")
            freeCompilerArgs += listOf("-Xopt-in=kotlinx.serialization.ExperimentalSerializationApi")
        }
    }

    withType<KotlinJvmCompile>().configureEach {
        kotlinOptions {
            jvmTarget = java_version
        }
    }

    withType<Test>().configureEach {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }
}
