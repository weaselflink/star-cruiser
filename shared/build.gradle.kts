@file:Suppress("PropertyName")

val kotlin_version: String by project
val ktor_version: String by project

plugins {
    kotlin("jvm") version "1.3.72"
    kotlin("plugin.serialization") version "1.3.72"
}

repositories {
    mavenLocal()
    jcenter()
    maven { url = uri("https://kotlin.bintray.com/ktor") }
}

group = "de.bissell.starcruiser"
version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(kotlin("stdlib-common"))
    implementation("io.ktor:ktor-serialization:$ktor_version")

    testImplementation("org.junit.jupiter:junit-jupiter:5.6.2")
    testImplementation("io.strikt:strikt-core:0.24.0")
}


tasks {
    test {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }

    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions.jvmTarget = "1.8"
    }
}
