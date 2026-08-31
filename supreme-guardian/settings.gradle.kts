pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolution {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "supreme-guardian"

include(":core:domain")
include(":core:incident-engine")
include(":core:suppression")
include(":core:evidence")
include(":edge")
include(":audio:supreme-dsp")
include(":integrations:thermal:flir")
include(":integrations:thermal:axis")
