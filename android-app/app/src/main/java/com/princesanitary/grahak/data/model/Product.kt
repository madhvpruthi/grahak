package com.princesanitary.grahak.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

data class Product(
    @DocumentId
    val id: String = "",
    
    @PropertyName("name")
    val name: String = "",
    
    @PropertyName("itemNumber")
    val itemNumber: String = "",

    @PropertyName("brand")
    val brand: String = "",

    @PropertyName("company")
    val company: String = "",
    
    @PropertyName("categoryId")
    val categoryId: String = "", // pipes | tanks | fittings | ceramics | tools | sanitaryware

    @PropertyName("itemType")
    val itemType: String = "", // CPVC | PVC | UPVC | SWR | BRASS | GI | CERAMIC | HDPE | STAINLESS_STEEL

    @PropertyName("size")
    val size: String = "", // 1/2", 3/4", 1", 1.5", 2", 1000L, etc.

    @PropertyName("unit")
    val unit: String = "Piece", // Piece, Metre, Length, Box, Set, Packet
    
    @PropertyName("mediaUrls")
    val mediaUrls: List<String> = emptyList(),
    
    @PropertyName("specifications")
    val specifications: Map<String, String> = emptyMap(),
    
    @PropertyName("pricingType")
    val pricingType: PricingType = PricingType.FIXED,
    
    @PropertyName("unitPrice")
    val unitPrice: Double = 0.0,
    
    @PropertyName("minOrderQty")
    val minOrderQty: Int = 1,
    
    @PropertyName("stockLevel")
    val stockLevel: Int = 0,
    
    /*
     * `@get:` is required, not decorative.
     *
     * Kotlin puts a bare @PropertyName on the backing field, but the Firestore
     * mapper reads the GETTER - and `isPublished()` is stripped to `published`
     * by JavaBeans convention. Without the get-site target, a document written
     * with `isPublished` would deserialize to `false`, and getProductById()
     * would reject every product as unpublished.
     */
    @get:PropertyName("isPublished")
    @set:PropertyName("isPublished")
    @PropertyName("isPublished")
    var isPublished: Boolean = false,

    @get:PropertyName("isFeatured")
    @set:PropertyName("isFeatured")
    @PropertyName("isFeatured")
    var isFeatured: Boolean = false,
    
    @PropertyName("tax")
    val tax: TaxInfo = TaxInfo()
) {
    val displayItemCode: String
        get() = if (itemNumber.isNotBlank()) itemNumber else "PS-${1000 + (kotlin.math.abs(id.hashCode()) % 9000)}"
}

enum class PricingType {
    @PropertyName("FIXED")
    FIXED,
    
    @PropertyName("RFQ_ONLY")
    RFQ_ONLY
}

data class TaxInfo(
    @PropertyName("hsn")
    val hsn: String = "",
    
    @PropertyName("gstRate")
    val gstRate: Double = 0.0
)

enum class ProductCategory(val id: String, val displayName: String) {
    CERAMICS("ceramics", "Wash Basins & Ceramics"),
    SEATS("seats", "Toilets & Commode Seats"),
    TAPS("taps", "Taps & Faucets"),
    SHOWERS("showers", "Showers & Bath"),
    MIRRORS("mirrors", "Mirrors & Vanities"),
    PIPES("pipes", "Pipes & Plumbing"),
    FITTINGS("fittings", "Pipe Fittings"),
    TANKS("tanks", "Water Tanks"),
    DRAINS("drains", "Drains & Traps"),
    HARDWARE("hardware", "Hardware & Adhesives"),
    SANITARYWARE("sanitaryware", "Sanitaryware"),
    TOOLS("tools", "Tools");
    
    companion object {
        fun fromId(id: String): ProductCategory? = values().find { it.id.equals(id, ignoreCase = true) }
    }
}
