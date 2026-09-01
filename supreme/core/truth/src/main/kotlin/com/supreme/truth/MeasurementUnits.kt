package com.supreme.truth

interface MeasurementUnit {
    val symbol: String
    val name: String
}

object Celsius : MeasurementUnit { override val symbol = "°C"; override val name = "Celsius" }
object Fahrenheit : MeasurementUnit { override val symbol = "°F"; override val name = "Fahrenheit" }
object Kelvin : MeasurementUnit { override val symbol = "K"; override val name = "Kelvin" }
object Bar : MeasurementUnit { override val symbol = "bar"; override val name = "Bar" }
object Pascal : MeasurementUnit { override val symbol = "Pa"; override val name = "Pascal" }
object Psi : MeasurementUnit { override val symbol = "psi"; override val name = "PSI" }
object LitersPerMinute : MeasurementUnit { override val symbol = "L/min"; override val name = "Liters per minute" }
object GallonsPerMinute : MeasurementUnit { override val symbol = "gal/min"; override val name = "Gallons per minute" }
object Volts : MeasurementUnit { override val symbol = "V"; override val name = "Volts" }
object Amperes : MeasurementUnit { override val symbol = "A"; override val name = "Amperes" }
object Watts : MeasurementUnit { override val symbol = "W"; override val name = "Watts" }
object Ohms : MeasurementUnit { override val symbol = "Ω"; override val name = "Ohms" }
object Rpm : MeasurementUnit { override val symbol = "RPM"; override val name = "Revolutions per minute" }
object MetersPerSecond : MeasurementUnit { override val symbol = "m/s"; override val name = "Meters per second" }
object MetersPerSecondSquared : MeasurementUnit { override val symbol = "m/s²"; override val name = "Meters per second squared" }
object GForce : MeasurementUnit { override val symbol = "G"; override val name = "G-force" }
object NewtonMeters : MeasurementUnit { override val symbol = "N·m"; override val name = "Newton-meters" }
object Hertz : MeasurementUnit { override val symbol = "Hz"; override val name = "Hertz" }
object Decibels : MeasurementUnit { override val symbol = "dB"; override val name = "Decibels" }
object DecibelsSPL : MeasurementUnit { override val symbol = "dB SPL"; override val name = "Decibels SPL" }
object Seconds : MeasurementUnit { override val symbol = "s"; override val name = "Seconds" }
object Milliseconds : MeasurementUnit { override val symbol = "ms"; override val name = "Milliseconds" }
object MegabitsPerSecond : MeasurementUnit { override val symbol = "Mbps"; override val name = "Megabits per second" }
object Dbm : MeasurementUnit { override val symbol = "dBm"; override val name = "dBm" }
object Percent : MeasurementUnit { override val symbol = "%"; override val name = "Percent" }
object Ratio : MeasurementUnit { override val symbol = ""; override val name = "Ratio" }
object Count : MeasurementUnit { override val symbol = ""; override val name = "Count" }
