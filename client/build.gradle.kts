@file:Suppress("PropertyName", "SuspiciousCollectionReassignment")

val kotlin_version: String by project

plugins {
    kotlin("multiplatform")
}

kotlin {
    js(IR) {
        useCommonJs()
        browser {}
        binaries.executable()
    }
}

dependencies {
    commonMainImplementation(projects.shared)
}
