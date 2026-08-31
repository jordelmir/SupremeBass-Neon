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

rootProject.name = "supreme"

include(":core:universal-model")
include(":core:device-abstraction")

include(":modules:fix-ai")
include(":modules:maintenance-os")
include(":modules:warranty-vault")
include(":modules:network-doctor")
include(":modules:noise-doctor")
include(":modules:vibration-doctor")
include(":modules:camera-hub")
include(":modules:find")
include(":modules:home-hub")
include(":modules:utilities")
include(":modules:inventory")
include(":modules:vehicle-hub")
include(":modules:leak-watch")
include(":modules:emergency")

include(":apps:android")
