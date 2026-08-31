package com.supreme.vehicle

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class VehicleHubEngineTest {

    private val engine = VehicleHubEngine()

    @Test
    fun `test add vehicle`() {
        val vehicle = Vehicle(
            id = "car-1",
            name = "Toyota Corolla",
            make = "Toyota",
            model = "Corolla",
            year = 2020
        )
        engine.addVehicle(vehicle)
        assertEquals(1, engine.state.value.vehicles.size)
    }

    @Test
    fun `test connect to OBD`() = runBlocking {
        engine.addVehicle(Vehicle(id = "car-1", name = "My Car"))
        val result = engine.connect("car-1")
        assertTrue(result)
        assertTrue(engine.state.value.vehicles.first { it.id == "car-1" }.connected)
    }

    @Test
    fun `test read DTCs`() = runBlocking {
        engine.addVehicle(Vehicle(id = "car-1", name = "My Car"))
        engine.connect("car-1")
        val dtcs = engine.readDTCs("car-1")
        assertNotNull(dtcs)
    }

    @Test
    fun `test maintenance schedule`() {
        engine.addVehicle(Vehicle(id = "car-1", name = "My Car", odometerKm = 50000.0))
        val tasks = engine.getMaintenanceSchedule("car-1")
        assertTrue(tasks.isNotEmpty())
        assertTrue(tasks.any { it.title.contains("Oil", ignoreCase = true) })
    }

    @Test
    fun `test fuel economy`() {
        val trips = listOf(
            TripData(distanceKm = 100.0, fuelLiters = 8.0, durationMinutes = 60, averageSpeedKmh = 100.0, timestamp = java.time.Instant.now()),
            TripData(distanceKm = 200.0, fuelLiters = 15.0, durationMinutes = 120, averageSpeedKmh = 100.0, timestamp = java.time.Instant.now())
        )
        val economy = engine.calculateFuelEconomy("car-1", trips)
        assertEquals(300.0, economy.totalDistanceKm)
        assertEquals(23.0, economy.totalFuelLiters)
    }
}
