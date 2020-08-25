
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
    kotlin("multiplatform") apply false
    kotlin("jvm") apply false
    kotlin("js") apply false
    kotlin("plugin.serialization") apply false
    id("com.github.johnrengelman.shadow") apply false
    id("com.github.ben-manes.versions")
    id("org.jlleitschuh.gradle.ktlint")
}

allprojects {
    group = "de.stefanbissell.starcruiser"
    version = "0.22.0"

    apply(plugin = "org.jetbrains.kotlin.plugin.serialization")
    apply(plugin = "com.github.ben-manes.versions")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    repositories {
        mavenCentral()
        jcenter()
        maven(url = "https://kotlin.bintray.com/ktor")
        maven(url = "https://jitpack.io")
    }

    tasks {
        dependencyUpdates {
            rejectVersionIf {
                isNonStable(candidate.version)
            }
        }
    }
}

ktlint {
    version.set("0.38.1")
}
