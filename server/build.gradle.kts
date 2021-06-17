@file:Suppress("PropertyName", "SuspiciousCollectionReassignment")

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val logback_version: String by project
val ktor_version: String by project
val kotlin_version: String by project
val kotlin_serialization_version: String by project
val kotlin_coroutine_version: String by project
val junit_version: String by project
val strikt_version: String by project

plugins {
    application
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.github.johnrengelman.shadow")
}

application {
    mainClass.set("io.ktor.server.netty.EngineMain")
}

dependencies {
    implementation(projects.shared)

    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlin_serialization_version")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.2.1")
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-websockets:$ktor_version")
    implementation("io.ktor:ktor-html-builder:$ktor_version")
    implementation("io.ktor:ktor-serialization:$ktor_version")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$kotlin_coroutine_version")
    implementation("org.jbox2d:jbox2d-library:2.2.1.1")

    testImplementation("org.junit.jupiter:junit-jupiter:$junit_version")
    testImplementation("io.ktor:ktor-server-tests:$ktor_version")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$kotlin_coroutine_version")
    testImplementation("io.strikt:strikt-core:$strikt_version")
    testImplementation("io.mockk:mockk:1.11.0")
}

tasks {
    test {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }

    withType<KotlinCompile>().configureEach {
        kotlinOptions {
            jvmTarget = "11"
            freeCompilerArgs += listOf("-Xopt-in=kotlin.RequiresOptIn")
            freeCompilerArgs += listOf("-Xopt-in=kotlin.time.ExperimentalTime")
        }
    }

    withType<Jar> {
        manifest {
            attributes(
                mapOf(
                    "Main-Class" to application.mainClass.get()
                )
            )
        }
        archiveBaseName.set("star-cruiser")
        archiveVersion.set("")
    }

    withType<ProcessResources> {
        dependsOn(project(":client").tasks["browserProductionWebpack"])

        from(project(":client").buildDir.resolve("distributions")) {
            into("js")
        }
    }
}
