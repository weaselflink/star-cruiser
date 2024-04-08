plugins {
    application
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.serialization)
    alias(libs.plugins.shadow)
}

kotlin {
    jvmToolchain(libs.versions.java.get().toInt())
}

application {
    mainClass.set("io.ktor.server.netty.EngineMain")
}

dependencies {
    implementation(project(path = ":shared", configuration = "jvmRuntimeElements"))

    implementation(libs.kotlin.stdlib)
    implementation(libs.bundles.kotlin.server)
    implementation(libs.bundles.ktor)
    implementation(libs.jbox2d)

    testImplementation(libs.bundles.test.base)
    testImplementation(libs.bundles.test.ktor)
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
