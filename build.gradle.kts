
plugins {
    kotlin("multiplatform") version "1.3.72" apply false
    kotlin("jvm") version "1.3.72" apply false
    kotlin("js") version "1.3.72" apply false
    kotlin("plugin.serialization") version "1.3.72" apply false
    id("com.github.johnrengelman.shadow") version "5.0.0" apply false
}

group = "de.bissell.starcruiser"
version = "0.0.1-SNAPSHOT"
