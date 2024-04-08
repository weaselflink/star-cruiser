plugins {
    alias(libs.plugins.kotlin.multiplatform)
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
