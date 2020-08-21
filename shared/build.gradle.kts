@file:Suppress("PropertyName", "SuspiciousCollectionReassignment", "UNUSED_VARIABLE")

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val kotlin_version: String by project
val ktor_version: String by project
val kotlin_serialization_version: String by project

plugins {
    kotlin("multiplatform")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$kotlin_serialization_version")
            }
        }
    }

    jvm {
        jvm()

        compilations["main"].defaultSourceSet {
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
                implementation(kotlin("reflect"))
            }
        }

        compilations["test"].defaultSourceSet {
            dependencies {
                implementation("org.junit.jupiter:junit-jupiter:5.7.0-M1")
                implementation("io.strikt:strikt-core:0.27.0")
            }
        }

        val main by compilations.getting {
            compileKotlinTask
            output
        }

        val test by compilations.getting {
            compileKotlinTask
            output
        }
    }

    js {
        browser()
    }
}

tasks {
    withType<KotlinCompile>().all {
        kotlinOptions {
            jvmTarget = "1.8"
            freeCompilerArgs += listOf("-Xopt-in=kotlin.ExperimentalUnsignedTypes")
            freeCompilerArgs += listOf("-Xopt-in=kotlinx.serialization.ExperimentalSerializationApi")
        }
    }

    withType<org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompile>().configureEach {
        kotlinOptions {
            freeCompilerArgs += listOf("-Xopt-in=kotlin.ExperimentalUnsignedTypes")
            freeCompilerArgs += listOf("-Xopt-in=kotlinx.serialization.ExperimentalSerializationApi")
        }
    }

    withType<org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompile>().configureEach {
        kotlinOptions {
            jvmTarget = "1.8"
            freeCompilerArgs += listOf("-Xopt-in=kotlin.ExperimentalUnsignedTypes")
            freeCompilerArgs += listOf("-Xopt-in=kotlinx.serialization.ExperimentalSerializationApi")
        }
    }

    withType<org.jetbrains.kotlin.gradle.dsl.KotlinJsCompile>().configureEach {
        kotlinOptions {
            freeCompilerArgs += listOf("-Xopt-in=kotlin.ExperimentalUnsignedTypes")
            freeCompilerArgs += listOf("-Xopt-in=kotlinx.serialization.ExperimentalSerializationApi")
        }
    }

    withType<Test>().configureEach {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }
}
