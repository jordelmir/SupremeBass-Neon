plugins {
    kotlin("jvm") version "1.9.20"
}

group = "com.supremeguardian"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":core:domain"))
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

sourceSets {
    main {
        kotlin.srcDir("src/main/kotlin")
    }
    test {
        kotlin.srcDir("src/test/kotlin")
    }
}
