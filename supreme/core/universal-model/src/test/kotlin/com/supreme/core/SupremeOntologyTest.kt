package com.supreme.core

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class SupremeOntologyTest {

    @Test
    fun `test AssetId generation is unique`() {
        val id1 = AssetId.generate()
        val id2 = AssetId.generate()
        assertNotEquals(id1.value, id2.value)
    }

    @Test
    fun `test DeviceId generation is unique`() {
        val id1 = DeviceId.generate()
        val id2 = DeviceId.generate()
        assertNotEquals(id1.value, id2.value)
    }

    @Test
    fun `test SupremeState getAssetById`() {
        val asset = Asset(
            id = AssetId("test-1"),
            name = "Test Asset",
            category = AssetCategory.APPLIANCE
        )
        val state = SupremeState(assets = listOf(asset))
        assertEquals(asset, state.getAssetById(AssetId("test-1")))
        assertNull(state.getAssetById(AssetId("nonexistent")))
    }

    @Test
    fun `test SupremeState getAssetsByCategory`() {
        val assets = listOf(
            Asset(id = AssetId("1"), name = "Fridge", category = AssetCategory.APPLIANCE),
            Asset(id = AssetId("2"), name = "TV", category = AssetCategory.ELECTRONICS),
            Asset(id = AssetId("3"), name = "Washer", category = AssetCategory.APPLIANCE)
        )
        val state = SupremeState(assets = assets)
        assertEquals(2, state.getAssetsByCategory(AssetCategory.APPLIANCE).size)
        assertEquals(1, state.getAssetsByCategory(AssetCategory.ELECTRONICS).size)
    }

    @Test
    fun `test SupremeState getTotalAssetValue`() {
        val assets = listOf(
            Asset(id = AssetId("1"), name = "A", category = AssetCategory.APPLIANCE, purchasePrice = 100.0),
            Asset(id = AssetId("2"), name = "B", category = AssetCategory.ELECTRONICS, purchasePrice = 200.0)
        )
        val state = SupremeState(assets = assets)
        assertEquals(300.0, state.getTotalAssetValue())
    }

    @Test
    fun `test Asset with all fields`() {
        val asset = Asset(
            id = AssetId("test"),
            name = "Washing Machine",
            category = AssetCategory.APPLIANCE,
            subcategory = "Front Load",
            brand = "Samsung",
            model = "WF45R6100AW",
            serialNumber = "SN123456",
            purchasePrice = 450000.0,
            currency = "CRC",
            condition = AssetCondition.GOOD
        )
        assertEquals("Washing Machine", asset.name)
        assertEquals(AssetCategory.APPLIANCE, asset.category)
        assertEquals(450000.0, asset.purchasePrice)
    }

    @Test
    fun `test Device with capabilities`() {
        val device = Device(
            id = DeviceId("dev-1"),
            name = "Thermal Camera",
            type = DeviceType.THERMAL_CAMERA,
            protocol = DeviceProtocol.ONVIF,
            capabilities = setOf(
                Capability.CanObserve(SensorType.TEMPERATURE),
                Capability.CanObserve(SensorType.CAMERA_THERMAL),
                Capability.CanCommunicate(CommunicationType.CAMERA_CAPTURE)
            )
        )
        assertEquals(3, device.capabilities.size)
        assertTrue(device.capabilities.any { it is Capability.CanObserve && it.sensorType == SensorType.TEMPERATURE })
    }

    @Test
    fun `test Observation with readings`() {
        val obs = Observation(
            id = ObservationId.generate(),
            deviceId = DeviceId("dev-1"),
            assetId = AssetId("asset-1"),
            sensorType = SensorType.TEMPERATURE,
            timestamp = java.time.Instant.now(),
            readings = mapOf("celsius" to 25.5, "humidity" to 60.0)
        )
        assertEquals(25.5, obs.readings["celsius"])
        assertEquals(60.0, obs.readings["humidity"])
    }

    @Test
    fun `test MaintenanceTask priority`() {
        val task = MaintenanceTask(
            id = MaintenanceId.generate(),
            assetId = AssetId("asset-1"),
            title = "Clean filters",
            description = "Clean AC filters",
            type = MaintenanceType.CLEANING,
            priority = Priority.HIGH,
            scheduledDate = java.time.Instant.now(),
            dueDate = java.time.Instant.now()
        )
        assertEquals(Priority.HIGH, task.priority)
        assertEquals(MaintenanceType.CLEANING, task.type)
    }
}
