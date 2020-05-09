
group = "de.bissell"
version = "0.0.1-SNAPSHOT"

allprojects {

    repositories {
        mavenLocal()
        jcenter()
        maven { url = uri("https://kotlin.bintray.com/ktor") }
    }
}
