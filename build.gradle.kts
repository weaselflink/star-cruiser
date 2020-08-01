import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

fun isNonStable(version: String): Boolean {
    val stableKeywords = listOf("release", "final", "ga").any { version.toLowerCase().contains(it) }
    val unstableKeywords = listOf("alpha", "dev").any { version.toLowerCase().contains(it) }
    val stableRegex = "^[0-9,.v-]+(-r)?$".toRegex()
    val unstableRegex = "^.*M\\d.*$".toRegex()
    val isStable = (stableKeywords || stableRegex.matches(version)) &&
        !(unstableKeywords || unstableRegex.matches(version))
    return isStable.not()
}

plugins {
    kotlin("multiplatform") version "1.3.72" apply false
    kotlin("jvm") version "1.3.72" apply false
    kotlin("js") version "1.3.72" apply false
    kotlin("plugin.serialization") version "1.3.72" apply false
    id("com.github.johnrengelman.shadow") version "6.0.0" apply false
    id("com.github.ben-manes.versions") version "0.29.0"
    id("org.jlleitschuh.gradle.ktlint") version "9.3.0"
}

allprojects {
    group = "de.stefanbissell.starcruiser"
    version = "0.18.0"

    repositories {
        mavenCentral()
        jcenter()
        maven(url = "https://kotlin.bintray.com/ktor")
        maven(url = "https://jitpack.io")
    }

    tasks {
        withType<DependencyUpdatesTask> {
            rejectVersionIf {
                isNonStable(candidate.version)
            }
        }
    }
}

ktlint {
    version.set("0.37.2")
}
