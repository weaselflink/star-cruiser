
plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.serialization) apply false
    alias(libs.plugins.shadow) apply false
    alias(libs.plugins.versions)
    alias(libs.plugins.version.filter)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.test.logger)
}

allprojects {
    group = "de.stefanbissell.starcruiser"
    version = "0.41.0"

    apply(plugin = rootProject.libs.plugins.versions.get().pluginId)
    apply(plugin = rootProject.libs.plugins.version.filter.get().pluginId)
    apply(plugin = rootProject.libs.plugins.ktlint.get().pluginId)
    apply(plugin = rootProject.libs.plugins.test.logger.get().pluginId)

    repositories {
        mavenCentral()
    }

    testlogger {
        setTheme("mocha")
    }
}
