package com.supreme.inventory

import com.supreme.core.*
import kotlinx.coroutines.flow.*
import java.time.Instant

/**
 * Supreme Inventory — know what you have and what it's worth.
 *
 * Scan objects with camera/barcode/QR/NFC/OCR.
 * Track: serial, purchase proof, photos, condition, warranty, estimated value.
 *
 * Useful for: insurance, theft, moving, maintenance, warranty, resale.
 */

class InventoryEngine {

    private val items = mutableMapOf<String, InventoryItem>()
    private val categories = mutableMapOf<String, InventoryCategory>()
    private val _state = MutableStateFlow(InventoryState())
    val state: StateFlow<InventoryState> = _state.asStateFlow()

    /**
     * Add an item to inventory.
     */
    fun addItem(item: InventoryItem) {
        items[item.id] = item
        updateState()
    }

    /**
     * Remove an item.
     */
    fun removeItem(itemId: String) {
        items.remove(itemId)
        updateState()
    }

    /**
     * Update item condition.
     */
    fun updateCondition(itemId: String, condition: AssetCondition, notes: String? = null) {
        val item = items[itemId] ?: return
        items[itemId] = item.copy(
            condition = condition,
            conditionNotes = notes,
            updatedAt = Instant.now()
        )
        updateState()
    }

    /**
     * Get all items by category.
     */
    fun getItemsByCategory(category: AssetCategory): List<InventoryItem> {
        return items.values.filter { it.category == category }
    }

    /**
     * Get total inventory value.
     */
    fun getTotalValue(): Double {
        return items.values.sumOf { it.purchasePrice ?: 0.0 }
    }

    /**
     * Get value by category.
     */
    fun getValueByCategory(): Map<AssetCategory, Double> {
        return items.values.groupBy { it.category }
            .mapValues { (_, items) -> items.sumOf { it.purchasePrice ?: 0.0 } }
    }

    /**
     * Get items with active warranties.
     */
    fun getItemsWithWarranty(): List<InventoryItem> {
        val now = Instant.now()
        return items.values.filter {
            it.warrantyExpiry != null && it.warrantyExpiry.isAfter(now)
        }
    }

    /**
     * Get items needing maintenance.
     */
    fun getItemsNeedingMaintenance(): List<InventoryItem> {
        return items.values.filter { it.needsMaintenance }
    }

    /**
     * Search items.
     */
    fun search(query: String): List<InventoryItem> {
        val lowerQuery = query.lowercase()
        return items.values.filter {
            it.name.lowercase().contains(lowerQuery) ||
                    it.brand?.lowercase()?.contains(lowerQuery) == true ||
                    it.model?.lowercase()?.contains(lowerQuery) == true ||
                    it.serialNumber?.lowercase()?.contains(lowerQuery) == true
        }
    }

    /**
     * Generate insurance report.
     */
    fun generateInsuranceReport(): InsuranceReport {
        val totalValue = getTotalValue()
        val byCategory = getValueByCategory()

        return InsuranceReport(
            timestamp = Instant.now(),
            totalItems = items.size,
            totalValue = totalValue,
            valueByCategory = byCategory,
            items = items.values.toList(),
            currency = "CRC"
        )
    }

    /**
     * Generate inventory export.
     */
    fun exportInventory(): InventoryExport {
        return InventoryExport(
            timestamp = Instant.now(),
            items = items.values.toList(),
            totalItems = items.size,
            totalValue = getTotalValue(),
            categories = getValueByCategory().map { (cat, value) ->
                CategorySummary(cat, items.values.count { it.category == cat }, value)
            }
        )
    }

    /**
     * Scan barcode/QR.
     */
    suspend fun scanBarcode(imageBytes: ByteArray): BarcodeResult {
        // TODO: Use ML Kit barcode scanning
        return BarcodeResult(
            barcode = "",
            format = "",
            success = false,
            error = "Barcode scanning not yet implemented"
        )
    }

    /**
     * Scan NFC tag.
     */
    suspend fun scanNFC(nfcData: ByteArray): NFCResult {
        // TODO: Use Android NFC
        return NFCResult(
            serialNumber = "",
            ndefMessages = emptyList(),
            success = false,
            error = "NFC scanning not yet implemented"
        )
    }

    private fun updateState() {
        _state.value = InventoryState(
            items = items.values.toList(),
            totalItems = items.size,
            totalValue = getTotalValue(),
            categories = getValueByCategory()
        )
    }
}

// ─────────────────────────────────────────────────────────────
// DATA CLASSES
// ─────────────────────────────────────────────────────────────

data class InventoryItem(
    val id: String,
    val name: String,
    val category: AssetCategory,
    val subcategory: String? = null,
    val brand: String? = null,
    val model: String? = null,
    val serialNumber: String? = null,
    val purchaseDate: Instant? = null,
    val purchasePrice: Double? = null,
    val currency: String = "CRC",
    val warrantyExpiry: Instant? = null,
    val condition: AssetCondition = AssetCondition.GOOD,
    val conditionNotes: String? = null,
    val location: String? = null,
    val photos: List<String> = emptyList(),
    val documents: List<String> = emptyList(),
    val barcode: String? = null,
    val nfcTag: String? = null,
    val needsMaintenance: Boolean = false,
    val estimatedReplacementValue: Double? = null,
    val notes: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)

data class InventoryCategory(
    val id: String,
    val name: String,
    val icon: String? = null
)

data class InventoryState(
    val items: List<InventoryItem> = emptyList(),
    val totalItems: Int = 0,
    val totalValue: Double = 0.0,
    val categories: Map<AssetCategory, Double> = emptyMap()
)

data class InsuranceReport(
    val timestamp: Instant,
    val totalItems: Int,
    val totalValue: Double,
    val valueByCategory: Map<AssetCategory, Double>,
    val items: List<InventoryItem>,
    val currency: String
)

data class InventoryExport(
    val timestamp: Instant,
    val items: List<InventoryItem>,
    val totalItems: Int,
    val totalValue: Double,
    val categories: List<CategorySummary>
)

data class CategorySummary(
    val category: AssetCategory,
    val count: Int,
    val value: Double
)

data class BarcodeResult(
    val barcode: String,
    val format: String,
    val success: Boolean,
    val error: String? = null
)

data class NFCResult(
    val serialNumber: String,
    val ndefMessages: List<String>,
    val success: Boolean,
    val error: String? = null
)
