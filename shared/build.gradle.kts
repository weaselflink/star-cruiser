@file:Suppress("PropertyName", "SuspiciousCollectionReassignment", "UNUSED_VARIABLE")

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val kotlin_version: String by project
val ktor_version: String by project

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-common:0.20.0")
            }
        }
    }

    jvm {
        jvm()

        compilations["main"].defaultSourceSet {
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
                implementation("io.ktor:ktor-serialization:$ktor_version")
            }
        }

        compilations["test"].defaultSourceSet {
            dependencies {
                implementation("org.junit.jupiter:junit-jupiter:5.7.0-M1")
                implementation("io.strikt:strikt-core:0.26.1")
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

        compilations["main"].defaultSourceSet {
            dependencies {
                implementation(kotlin("stdlib-js"))
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-js:0.20.0")
            }
        }
    }
}

tasks {
    withType<KotlinCompile>().all {
        kotlinOptions {
            jvmTarget = "1.8"
            freeCompilerArgs += listOf("-Xopt-in=kotlin.ExperimentalUnsignedTypes")
        }
    }

    withType<org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompile>().configureEach {
        kotlinOptions {
            freeCompilerArgs += listOf("-Xopt-in=kotlin.ExperimentalUnsignedTypes")
        }
    }

    withType<org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompile>().configureEach {
        kotlinOptions {
            jvmTarget = "1.8"
            freeCompilerArgs += listOf("-Xopt-in=kotlin.ExperimentalUnsignedTypes")
        }
    }

    withType<org.jetbrains.kotlin.gradle.dsl.KotlinJsCompile>().configureEach {
        kotlinOptions {
            freeCompilerArgs += listOf("-Xopt-in=kotlin.ExperimentalUnsignedTypes")
        }
    }

    withType<Test>().configureEach {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }
}
