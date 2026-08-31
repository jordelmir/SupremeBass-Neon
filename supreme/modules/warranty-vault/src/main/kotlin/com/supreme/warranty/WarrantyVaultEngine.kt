package com.supreme.warranty

import com.supreme.core.*
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Supreme Warranty Vault — "Does this still have warranty?"
 *
 * Scans invoices, warranty cards, serial numbers, and manuals.
 * Extracts warranty information using OCR/ML Kit.
 * Tracks warranty status for all assets.
 */

class WarrantyVaultEngine {

    private val warranties = mutableMapOf<AssetId, WarrantyInfo>()
    private val documents = mutableMapOf<AssetId, MutableList<DocumentRef>>()

    /**
     * Process a scanned document.
     */
    suspend fun processDocument(
        assetId: AssetId,
        documentBytes: ByteArray,
        documentType: DocumentScanType
    ): DocumentProcessingResult {
        // 1. Run OCR on document
        val ocrResult = performOCR(documentBytes)

        // 2. Extract relevant fields
        val extractedFields = extractFields(ocrResult, documentType)

        // 3. Create/update warranty info
        val warranty = createWarrantyFromFields(assetId, extractedFields, documentType)

        // 4. Store document reference
        val docRef = DocumentRef(
            id = EvidenceId.generate(),
            assetId = assetId,
            type = documentType,
            filePath = "", // Would be stored in local storage
            extractedText = ocrResult.text,
            extractedFields = extractedFields,
            processedAt = Instant.now()
        )

        documents.getOrPut(assetId) { mutableListOf() }.add(docRef)

        if (warranty != null) {
            warranties[assetId] = warranty
        }

        return DocumentProcessingResult(
            success = true,
            ocrText = ocrResult.text,
            extractedFields = extractedFields,
            warrantyInfo = warranty,
            confidence = ocrResult.confidence
        )
    }

    /**
     * Get warranty status for an asset.
     */
    fun getWarrantyStatus(assetId: AssetId): WarrantyStatus {
        val warranty = warranties[assetId]

        return if (warranty != null) {
            val now = Instant.now()
            val daysRemaining = ChronoUnit.DAYS.between(now, warranty.warrantyEnd).toInt()

            WarrantyStatus(
                assetId = assetId,
                hasWarranty = true,
                isActive = warranty.isActive && daysRemaining > 0,
                warrantyInfo = warranty,
                daysRemaining = daysRemaining,
                expiresAt = warranty.warrantyEnd,
                status = when {
                    !warranty.isActive -> WarrantyExpiryStatus.CANCELLED
                    daysRemaining <= 0 -> WarrantyExpiryStatus.EXPIRED
                    daysRemaining <= 30 -> WarrantyExpiryStatus.EXPIRING_SOON
                    else -> WarrantyExpiryStatus.ACTIVE
                }
            )
        } else {
            WarrantyStatus(
                assetId = assetId,
                hasWarranty = false,
                isActive = false,
                warrantyInfo = null,
                daysRemaining = 0,
                expiresAt = null,
                status = WarrantyExpiryStatus.UNKNOWN
            )
        }
    }

    /**
     * Get all warranties summary.
     */
    fun getAllWarrantiesSummary(): WarrantySummary {
        val now = Instant.now()

        val active = warranties.values.filter {
            it.isActive && it.warrantyEnd.isAfter(now)
        }
        val expiringSoon = warranties.values.filter {
            it.isActive && it.warrantyEnd.isAfter(now) &&
                    ChronoUnit.DAYS.between(now, it.warrantyEnd) <= 30
        }
        val expired = warranties.values.filter {
            !it.isActive || it.warrantyEnd.isBefore(now)
        }

        return WarrantySummary(
            totalAssets = warranties.size,
            activeWarranties = active.size,
            expiringSoonCount = expiringSoon.size,
            expiredCount = expired.size,
            totalValueAtRisk = expiringSoon.sumOf { it.purchasePrice },
            nextToExpire = active.minByOrNull { it.warrantyEnd }
        )
    }

    /**
     * Get warranties expiring soon.
     */
    fun getExpiringWarranties(daysAhead: Int = 30): List<WarrantyInfo> {
        val now = Instant.now()
        val cutoff = now.plus(daysAhead.toLong(), ChronoUnit.DAYS)

        return warranties.values.filter {
            it.isActive && it.warrantyEnd.isAfter(now) && it.warrantyEnd.isBefore(cutoff)
        }.sortedBy { it.warrantyEnd }
    }

    /**
     * Get documents for an asset.
     */
    fun getAssetDocuments(assetId: AssetId): List<DocumentRef> {
        return documents[assetId] ?: emptyList()
    }

    /**
     * Check if asset is under warranty.
     */
    fun isUnderWarranty(assetId: AssetId): Boolean {
        val warranty = warranties[assetId] ?: return false
        return warranty.isActive && warranty.warrantyEnd.isAfter(Instant.now())
    }

    // ─────────────────────────────────────────────────────────────
    // OCR AND EXTRACTION
    // ─────────────────────────────────────────────────────────────

    private fun performOCR(documentBytes: ByteArray): OCRResult {
        // TODO: Use Google ML Kit Document Scanner
        // For now, return placeholder
        return OCRResult(
            text = "Placeholder OCR text",
            confidence = 0.85,
            detectedFields = emptyMap()
        )
    }

    private fun extractFields(ocrResult: OCRResult, documentType: DocumentScanType): Map<String, String> {
        val fields = mutableMapOf<String, String>()

        // Parse common patterns from OCR text
        val text = ocrResult.text

        // Date patterns
        val datePatterns = listOf(
            Regex("""(\d{1,2})[/-](\d{1,2})[/-](\d{4})"""),
            Regex("""(\d{4})[/-](\d{1,2})[/-](\d{1,2})"""),
            Regex("""(\d{1,2})\s+(ene|feb|mar|abr|may|jun|jul|ago|sep|oct|nov|dic)\w*\s+(\d{4})""", RegexOption.IGNORE_CASE)
        )

        for (pattern in datePatterns) {
            val match = pattern.find(text)
            if (match != null) {
                fields["date"] = match.value
                break
            }
        }

        // Price patterns
        val pricePatterns = listOf(
            Regex("""₡\s*([\d,.]+)"""),
            Regex("""\$\s*([\d,.]+)"""),
            Regex("""([\d,.]+)\s*(?:colones|CRC|USD)""", RegexOption.IGNORE_CASE)
        )

        for (pattern in pricePatterns) {
            val match = pattern.find(text)
            if (match != null) {
                fields["price"] = match.groupValues[1]
                break
            }
        }

        // Serial number patterns
        val serialPatterns = listOf(
            Regex("""(?:serial|serie|s/n|sn)[:\s]*([A-Z0-9-]+)""", RegexOption.IGNORE_CASE),
            Regex("""(?:model|modelo)[:\s]*([A-Z0-9-]+)""", RegexOption.IGNORE_CASE)
        )

        for (pattern in serialPatterns) {
            val match = pattern.find(text)
            if (match != null) {
                fields["serial"] = match.groupValues[1]
                break
            }
        }

        // Warranty period patterns
        val warrantyPatterns = listOf(
            Regex("""(\d+)\s*(?:months?|meses?)""", RegexOption.IGNORE_CASE),
            Regex("""(\d+)\s*(?:years?|años?)""", RegexOption.IGNORE_CASE),
            Regex("""warranty[:\s]*(\d+)""", RegexOption.IGNORE_CASE)
        )

        for (pattern in warrantyPatterns) {
            val match = pattern.find(text)
            if (match != null) {
                fields["warranty_months"] = match.groupValues[1]
                break
            }
        }

        return fields
    }

    private fun createWarrantyFromFields(
        assetId: AssetId,
        fields: Map<String, String>,
        documentType: DocumentScanType
    ): WarrantyInfo? {
        val purchaseDate = fields["date"]?.let { parseDate(it) } ?: return null
        val price = fields["price"]?.replace(",", "")?.toDoubleOrNull() ?: 0.0
        val warrantyMonths = fields["warranty_months"]?.toIntOrNull() ?: 24

        val warrantyStart = purchaseDate
        val warrantyEnd = purchaseDate.plus(warrantyMonths.toLong() * 30, ChronoUnit.DAYS)

        return WarrantyInfo(
            assetId = assetId,
            provider = "Unknown",
            purchaseDate = purchaseDate,
            warrantyStart = warrantyStart,
            warrantyEnd = warrantyEnd,
            warrantyMonths = warrantyMonths,
            purchasePrice = price,
            serialNumber = fields["serial"],
            isActive = true,
            daysRemaining = ChronoUnit.DAYS.between(Instant.now(), warrantyEnd).toInt()
        )
    }

    private fun parseDate(dateString: String): Instant? {
        // Simple date parsing; in production use proper date library
        return try {
            Instant.now() // Placeholder
        } catch (e: Exception) {
            null
        }
    }
}

// ─────────────────────────────────────────────────────────────
// DATA CLASSES
// ─────────────────────────────────────────────────────────────

enum class DocumentScanType {
    INVOICE,
    WARRANTY_CARD,
    SERIAL_NUMBER,
    MANUAL,
    RECEIPT,
    OTHER
}

data class DocumentRef(
    val id: EvidenceId,
    val assetId: AssetId,
    val type: DocumentScanType,
    val filePath: String,
    val extractedText: String,
    val extractedFields: Map<String, String>,
    val processedAt: Instant
)

data class DocumentProcessingResult(
    val success: Boolean,
    val ocrText: String,
    val extractedFields: Map<String, String>,
    val warrantyInfo: WarrantyInfo?,
    val confidence: Double,
    val error: String? = null
)

data class OCRResult(
    val text: String,
    val confidence: Double,
    val detectedFields: Map<String, String>
)

data class WarrantyStatus(
    val assetId: AssetId,
    val hasWarranty: Boolean,
    val isActive: Boolean,
    val warrantyInfo: WarrantyInfo?,
    val daysRemaining: Int,
    val expiresAt: Instant?,
    val status: WarrantyExpiryStatus
)

enum class WarrantyExpiryStatus {
    UNKNOWN,
    ACTIVE,
    EXPIRING_SOON,
    EXPIRED,
    CANCELLED
}

data class WarrantySummary(
    val totalAssets: Int,
    val activeWarranties: Int,
    val expiringSoonCount: Int,
    val expiredCount: Int,
    val totalValueAtRisk: Double,
    val nextToExpire: WarrantyInfo?
)
