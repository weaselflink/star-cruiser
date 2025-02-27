import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.serialization)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.bundles.kotlin.serialization)
            }
        }
    }

    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.fromTarget(libs.versions.java.get()))
        }

        compilations["main"].defaultSourceSet {
            dependencies {
                implementation(libs.kotlin.stdlib)
            }
        }

        compilations["test"].defaultSourceSet {
            dependencies {
                implementation(libs.bundles.test.base)
                runtimeOnly("org.junit.platform:junit-platform-launcher")
            }
        }
    }

    js(IR) {
        browser {}
        binaries.executable()
    }
}

tasks {
    withType<KotlinCompilationTask<*>>().all {
        compilerOptions {
            optIn.add("kotlin.ExperimentalUnsignedTypes")
            optIn.add("kotlinx.serialization.ExperimentalSerializationApi")
        }
    }

    withType<Test>().configureEach {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }
}
