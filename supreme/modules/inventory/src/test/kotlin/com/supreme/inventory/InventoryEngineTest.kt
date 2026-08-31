package com.supreme.inventory

import com.supreme.core.AssetCategory
import com.supreme.core.AssetCondition
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class InventoryEngineTest {

    private val engine = InventoryEngine()

    @Test
    fun `test add item`() {
        val item = InventoryItem(
            id = "item-1",
            name = "Samsung TV",
            category = AssetCategory.ELECTRONICS,
            purchasePrice = 350000.0
        )
        engine.addItem(item)
        assertEquals(1, engine.state.value.totalItems)
    }

    @Test
    fun `test total value`() {
        engine.addItem(InventoryItem(id = "1", name = "TV", category = AssetCategory.ELECTRONICS, purchasePrice = 350000.0))
        engine.addItem(InventoryItem(id = "2", name = "Fridge", category = AssetCategory.APPLIANCE, purchasePrice = 450000.0))
        assertEquals(800000.0, engine.getTotalValue())
    }

    @Test
    fun `test value by category`() {
        engine.addItem(InventoryItem(id = "1", name = "TV", category = AssetCategory.ELECTRONICS, purchasePrice = 350000.0))
        engine.addItem(InventoryItem(id = "2", name = "Fridge", category = AssetCategory.APPLIANCE, purchasePrice = 450000.0))
        val byCategory = engine.getValueByCategory()
        assertEquals(350000.0, byCategory[AssetCategory.ELECTRONICS])
        assertEquals(450000.0, byCategory[AssetCategory.APPLIANCE])
    }

    @Test
    fun `test search`() {
        engine.addItem(InventoryItem(id = "1", name = "Samsung TV", category = AssetCategory.ELECTRONICS))
        engine.addItem(InventoryItem(id = "2", name = "LG Fridge", category = AssetCategory.APPLIANCE))
        val results = engine.search("samsung")
        assertEquals(1, results.size)
        assertEquals("Samsung TV", results[0].name)
    }

    @Test
    fun `test update condition`() {
        engine.addItem(InventoryItem(id = "1", name = "TV", category = AssetCategory.ELECTRONICS))
        engine.updateCondition("1", AssetCondition.FAIR, "Minor scratch")
        val item = engine.state.value.items.first { it.id == "1" }
        assertEquals(AssetCondition.FAIR, item.condition)
        assertEquals("Minor scratch", item.conditionNotes)
    }

    @Test
    fun `test insurance report`() {
        engine.addItem(InventoryItem(id = "1", name = "TV", category = AssetCategory.ELECTRONICS, purchasePrice = 350000.0))
        val report = engine.generateInsuranceReport()
        assertEquals(1, report.totalItems)
        assertEquals(350000.0, report.totalValue)
    }
}
