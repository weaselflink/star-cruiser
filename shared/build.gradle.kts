import org.jetbrains.kotlin.gradle.dsl.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

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
        compilations.all {
            kotlinOptions {
                jvmTarget = libs.versions.java.get()
            }
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
    withType<KotlinCompile<*>>().all {
        kotlinOptions {
            freeCompilerArgs += listOf("-opt-in=kotlin.ExperimentalUnsignedTypes")
            freeCompilerArgs += listOf("-opt-in=kotlinx.serialization.ExperimentalSerializationApi")
        }
    }

    withType<KotlinJvmCompile>().configureEach {
        kotlinOptions {
            jvmTarget = libs.versions.java.get()
        }
    }

    withType<Test>().configureEach {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }
}
