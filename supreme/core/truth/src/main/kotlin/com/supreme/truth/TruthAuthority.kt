package com.supreme.truth

/**
 * Truth Authority — the source of truth for any data point.
 *
 * Every piece of data in Supreme MUST carry an authority.
 * This prevents synthetic/simulated data from being presented as real.
 *
 * Authority hierarchy (strongest → weakest):
 *   PHYSICAL_MEASURED > CALIBRATED_INSTRUMENT > DEVICE_REPORTED > USER_VERIFIED >
 *   USER_REPORTED > DERIVED > HEURISTIC > SIMULATED > UNKNOWN
 */
enum class TruthAuthority(val label: String, val trustworthy: Boolean) {
    CALIBRATED_INSTRUMENT("Calibrated instrument", true),
    PHYSICAL_MEASURED("Physical measurement", true),
    DEVICE_REPORTED("Device reported", true),
    USER_VERIFIED("User verified", true),
    USER_REPORTED("User reported", true),
    DERIVED("Derived", false),
    HEURISTIC("Heuristic", false),
    SIMULATED("Simulated", false),
    UNKNOWN("Unknown", false);

    val isTrustedForPhysical: Boolean get() = this in setOf(
        CALIBRATED_INSTRUMENT,
        PHYSICAL_MEASURED,
        DEVICE_REPORTED,
        USER_VERIFIED
    )
}
