
fun isNonStable(version: String): Boolean {
    return listOf("alpha", "dev").any { version.toLowerCase().contains(it) }
}

plugins {
    kotlin("multiplatform") apply false
    kotlin("jvm") apply false
    kotlin("js") apply false
    kotlin("plugin.serialization") apply false
    id("com.github.johnrengelman.shadow") apply false
    id("com.github.ben-manes.versions")
    id("org.jlleitschuh.gradle.ktlint")
    id("com.adarshr.test-logger")
}

allprojects {
    group = "de.stefanbissell.starcruiser"
    version = "0.40.0"

    apply(plugin = "com.github.ben-manes.versions")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "com.adarshr.test-logger")

    repositories {
        mavenCentral()
    }

    testlogger {
        setTheme("mocha")
    }

    tasks {
        dependencyUpdates {
            rejectVersionIf {
                isNonStable(candidate.version)
            }
        }
    }
}

subprojects {
    ktlint {
        version.set("0.46.1")
    }
}
