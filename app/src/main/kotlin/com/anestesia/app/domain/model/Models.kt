package com.anestesia.app.domain.model

import kotlinx.serialization.Serializable

/**
 * Categorías de fármacos según código de colores ASTM para anestesiología.
 */
enum class DrugCategory(val displayName: String, val colorHex: String) {
    RELAJANTE("Relajante Muscular", "#FF0000"),
    OPIOIDE("Opioide", "#0000FF"),
    HIPNOTICO("Hipnótico", "#CCCC00"),   // Amarillo oscurecido para contraste sobre blanco
    BENZODIACEPINA("Benzodiacepina", "#FF8C00"),
    OTRO("Otro", "#607D8B")
}

@Serializable
data class Drug(
    val id: Int = 0,
    val name: String,
    val category: String,
    val doseMgKg: Double,
    val concentrationMgMl: Double,
    val reinjectionTimeMinutes: Int,
    val antidote: String,
    val notes: String = "",
    val isActive: Boolean = true
) {
    fun calculateVolumeMl(weightKg: Double): Double {
        if (concentrationMgMl <= 0.0) return 0.0
        return (doseMgKg * weightKg) / concentrationMgMl
    }

    fun categoryEnum(): DrugCategory =
        DrugCategory.entries.firstOrNull { it.name == category } ?: DrugCategory.OTRO
}

data class ActiveTimer(
    val id: Int = 0,
    val drugId: Int,
    val drugName: String,
    val drugCategory: String,
    val antidote: String,
    val administeredAtMs: Long,
    val reinjectionTimeMs: Long,
    val calculatedVolumeMl: Double,
    val patientWeightKg: Double,
    val alertAt80Sent: Boolean = false,
    val alertAt100Sent: Boolean = false,
    val isExpired: Boolean = false
) {
    val elapsedMs: Long get() = System.currentTimeMillis() - administeredAtMs
    val remainingMs: Long get() = maxOf(0L, reinjectionTimeMs - elapsedMs)
    val progressFraction: Float get() = (elapsedMs.toFloat() / reinjectionTimeMs).coerceIn(0f, 1f)
    val isWarning: Boolean get() = progressFraction >= 0.80f && !isExpired
    val isCritical: Boolean get() = progressFraction >= 1.0f

    fun categoryEnum(): DrugCategory =
        DrugCategory.entries.firstOrNull { it.name == drugCategory } ?: DrugCategory.OTRO
}

@Serializable
data class VademecumBackup(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val drugs: List<Drug>
)
