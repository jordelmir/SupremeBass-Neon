plugins {
    kotlin("jvm")
}

group = "com.supreme"
version = "1.0-SNAPSHOT"

dependencies {
    implementation(project(":core:universal-model"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}
