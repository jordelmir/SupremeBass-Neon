plugins {
    kotlin("jvm")
}

group = "com.supreme"
version = "1.0-SNAPSHOT"

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}
