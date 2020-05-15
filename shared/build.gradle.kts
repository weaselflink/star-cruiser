@file:Suppress("PropertyName")

val kotlin_version: String by project
val ktor_version: String by project

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

repositories {
    mavenLocal()
    jcenter()
    maven { url = uri("https://kotlin.bintray.com/ktor") }
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
                implementation("org.junit.jupiter:junit-jupiter:5.6.2")
                implementation("io.strikt:strikt-core:0.24.0")
            }
        }

        val main by compilations.getting {
            kotlinOptions {
                jvmTarget = "1.8"
            }

            compileKotlinTask
            output
        }

        val test by compilations.getting {
            kotlinOptions {
                jvmTarget = "1.8"
            }

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

group = "de.bissell.starcruiser"
version = "0.0.1-SNAPSHOT"

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions.jvmTarget = "1.8"
    }

    withType<Test>().configureEach {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }
}
