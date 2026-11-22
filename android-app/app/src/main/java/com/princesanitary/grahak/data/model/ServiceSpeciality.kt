package com.princesanitary.grahak.data.model

/**
 * The work a plumber can take on.
 *
 * Each speciality carries a suggested visit rate. During onboarding the plumber
 * sees these as a starting point and sets their own number, which is what the
 * customer is later quoted. Storing the plumber's own figure per speciality (in
 * [PlumberRates.customRates], keyed by [id]) is what lets a customer compare
 * plumbers on price instead of being handed a single flat rate.
 */
enum class ServiceSpeciality(
    val id: String,
    val displayName: String,
    val description: String,
    val suggestedRate: Double
) {
    LEAK_REPAIR(
        id = "leak_repair",
        displayName = "Leak repair",
        description = "Taps, pipes, joints, seepage",
        suggestedRate = 250.0
    ),
    PIPE_FITTING(
        id = "pipe_fitting",
        displayName = "Pipe fitting",
        description = "New lines, replacements, rerouting",
        suggestedRate = 400.0
    ),
    BATHROOM_FITTING(
        id = "bathroom_fitting",
        displayName = "Bathroom fitting",
        description = "Basins, showers, taps, mixers",
        suggestedRate = 500.0
    ),
    TOILET_INSTALL(
        id = "toilet_install",
        displayName = "Toilet installation",
        description = "Western, Indian, flush tanks",
        suggestedRate = 700.0
    ),
    KITCHEN_PLUMBING(
        id = "kitchen_plumbing",
        displayName = "Kitchen plumbing",
        description = "Sinks, RO lines, drainage",
        suggestedRate = 350.0
    ),
    DRAIN_CLEANING(
        id = "drain_cleaning",
        displayName = "Drain & blockage",
        description = "Chokes, traps, sewer lines",
        suggestedRate = 300.0
    ),
    MOTOR_PUMP(
        id = "motor_pump",
        displayName = "Motor & pump",
        description = "Water pumps, boosters, tanks",
        suggestedRate = 600.0
    ),
    EMERGENCY(
        id = "emergency",
        displayName = "Emergency call-out",
        description = "Same-day urgent visits",
        suggestedRate = 800.0
    );

    companion object {
        fun fromId(id: String): ServiceSpeciality? = entries.find { it.id == id }
    }
}

/**
 * Where a plumber's application sits.
 *
 * A plumber can browse the app while PENDING but cannot accept work until the
 * store has approved them - the same gate Uber/Rapido put on a new driver.
 */
enum class VerificationStatus {
    PENDING,
    APPROVED,
    REJECTED;

    companion object {
        fun fromStored(value: String?): VerificationStatus = when (value) {
            "APPROVED", "VERIFIED" -> APPROVED
            "REJECTED" -> REJECTED
            else -> PENDING
        }
    }
}
