package com.princesanitary.grahak.ui.components

import com.princesanitary.grahak.data.model.Product
import kotlin.math.abs
import kotlin.math.roundToInt

data class ProductPricingInfo(
    val sellingPrice: Double,
    val mrp: Double,
    val discountPercent: Int,
    val showDiscountTag: Boolean,
    val tagText: String
)

/**
 * Shared, deterministic pricing logic so Dashboard and Catalog ALWAYS display
 * the exact same selling price, MRP, and discount tag.
 *
 * Rules:
 * 1. Ceramics: random % from 10 to 20 with "% OFF" tag shown.
 * 2. Best Deals: random % from 5 to 12 with "% OFF" tag shown.
 * 3. Remaining categories: random % (6 to 15) for MRP strikethrough, but NO tag shown.
 */
fun calculateProductPricing(
    product: Product,
    isCeramicsShelf: Boolean = false,
    isBestDealsShelf: Boolean = false
): ProductPricingInfo {
    val sellingPrice = product.unitPrice
    if (sellingPrice <= 0.0) {
        return ProductPricingInfo(
            sellingPrice = 0.0,
            mrp = 0.0,
            discountPercent = 0,
            showDiscountTag = false,
            tagText = ""
        )
    }

    // Deterministic pseudo-random seed based on product ID
    val seed = abs(product.id.hashCode())

    val isCeramics = isCeramicsShelf ||
        product.categoryId.equals("ceramics", true) ||
        product.categoryId.equals("seats", true) ||
        product.categoryId.equals("mirrors", true) ||
        product.categoryId.equals("sanitaryware", true) ||
        product.itemType.contains("basin", true) ||
        product.name.contains("basin", true) ||
        product.name.contains("commode", true) ||
        product.name.contains("seat", true)

    val isBestDeal = isBestDealsShelf || product.isFeatured

    val discountPercent: Int
    val showTag: Boolean

    if (isCeramics) {
        // 10% to 20%
        discountPercent = 10 + (seed % 11)
        showTag = true
    } else if (isBestDeal) {
        // 5% to 12%
        discountPercent = 5 + (seed % 8)
        showTag = true
    } else {
        // 6% to 15% - discount in MRP, but no tag badge
        discountPercent = 6 + (seed % 10)
        showTag = false
    }

    // MRP is calculated such that sellingPrice is (1 - discount/100) * MRP
    val mrp = (sellingPrice / (1.0 - (discountPercent / 100.0))).roundToInt().toDouble()

    return ProductPricingInfo(
        sellingPrice = sellingPrice,
        mrp = mrp,
        discountPercent = discountPercent,
        showDiscountTag = showTag,
        tagText = "$discountPercent% OFF"
    )
}
