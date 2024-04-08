@file:Suppress("PropertyName", "SuspiciousCollectionReassignment")

val java_version: String by project
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
    id("org.jlleitschuh.gradle.ktlint")
}

kotlin {
    jvmToolchain(java_version.toInt())
}

application {
    mainClass.set("io.ktor.server.netty.EngineMain")
}

dependencies {
    implementation(project(path = ":shared", configuration = "jvmRuntimeElements"))

    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlin_serialization_version")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
    implementation("io.ktor:ktor-server-websockets:$ktor_version")
    implementation("io.ktor:ktor-server-html-builder:$ktor_version")
    implementation("io.ktor:ktor-serialization:$ktor_version")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$kotlin_coroutine_version")
    implementation("org.jbox2d:jbox2d-library:2.2.1.1")

    testImplementation("org.junit.jupiter:junit-jupiter:$junit_version")
    testImplementation("io.ktor:ktor-server-tests:$ktor_version")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$kotlin_coroutine_version")
    testImplementation("io.strikt:strikt-core:$strikt_version")
    testImplementation("io.mockk:mockk:1.13.10")
}

tasks {
    test {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
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

    processResources {
        dependsOn(project(":client").tasks["jsBrowserDistribution"])
        dependsOn(project(":client").tasks["jsBrowserProductionWebpack"])

        from(project(":client").layout.buildDirectory.file("dist/js/productionExecutable")) {
            into("js")
        }
    }
}
