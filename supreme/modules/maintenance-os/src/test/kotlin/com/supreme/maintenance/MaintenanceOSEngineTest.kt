package com.supreme.maintenance

import com.supreme.core.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class MaintenanceOSEngineTest {

    private val engine = MaintenanceOSEngine()

    @Test
    fun `test add asset generates tasks`() {
        val asset = Asset(
            id = AssetId("fridge-1"),
            name = "Samsung Refrigerator",
            category = AssetCategory.APPLIANCE,
            subcategory = "refrigerator"
        )
        val schedule = engine.addAsset(asset)
        assertTrue(schedule.tasks.isNotEmpty())
    }

    @Test
    fun `test refrigerator gets condenser cleaning task`() {
        val asset = Asset(
            id = AssetId("fridge-1"),
            name = "Refrigerator",
            category = AssetCategory.APPLIANCE,
            subcategory = "refrigerator"
        )
        val schedule = engine.addAsset(asset)
        assertTrue(schedule.tasks.any { it.title.contains("condenser", ignoreCase = true) })
    }

    @Test
    fun `test washing machine gets filter cleaning task`() {
        val asset = Asset(
            id = AssetId("washer-1"),
            name = "Washing Machine",
            category = AssetCategory.APPLIANCE
        )
        val schedule = engine.addAsset(asset)
        assertTrue(schedule.tasks.any { it.title.contains("filter", ignoreCase = true) })
    }

    @Test
    fun `test vehicle gets oil change task`() {
        val asset = Asset(
            id = AssetId("car-1"),
            name = "Toyota Corolla",
            category = AssetCategory.VEHICLE
        )
        val schedule = engine.addAsset(asset)
        assertTrue(schedule.tasks.any { it.title.contains("oil", ignoreCase = true) })
    }

    @Test
    fun `test complete task removes from pending`() {
        val asset = Asset(
            id = AssetId("fridge-1"),
            name = "Refrigerator",
            category = AssetCategory.APPLIANCE,
            subcategory = "refrigerator"
        )
        val schedule = engine.addAsset(asset)
        val task = schedule.tasks.first()
        engine.completeTask(task.id)
        val updatedTasks = engine.getAssetTasks(AssetId("fridge-1"))
        assertFalse(updatedTasks.any { it.id == task.id && it.completedDate == null })
    }

    @Test
    fun `test get overdue tasks`() {
        val tasks = engine.getOverdueTasks()
        assertNotNull(tasks)
    }

    @Test
    fun `test get today tasks`() {
        val tasks = engine.getTodayTasks()
        assertNotNull(tasks)
    }

    @Test
    fun `test maintenance summary`() {
        val asset = Asset(
            id = AssetId("fridge-1"),
            name = "Refrigerator",
            category = AssetCategory.APPLIANCE,
            subcategory = "refrigerator"
        )
        engine.addAsset(asset)
        val summary = engine.getMaintenanceSummary()
        assertEquals(1, summary.totalAssets)
        assertTrue(summary.totalTasks > 0)
    }
}
