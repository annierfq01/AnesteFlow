package com.anestesia.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidad Room que representa un temporizador activo para un fármaco administrado.
 * Referencia al fármaco del vademécum mediante FK.
 */
@Entity(
    tableName = "active_timers",
    foreignKeys = [
        ForeignKey(
            entity = DrugEntity::class,
            parentColumns = ["id"],
            childColumns = ["drugId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["drugId"])]
)
data class ActiveTimerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val drugId: Int,
    val drugName: String,          // Cache local para evitar JOINs en hot path
    val drugCategory: String,
    val antidote: String,
    val administeredAtMs: Long,    // Timestamp de administración (System.currentTimeMillis)
    val reinjectionTimeMs: Long,   // Duración total en ms
    val calculatedVolumeMl: Double,// Volumen calculado al momento de la administración
    val patientWeightKg: Double,
    val alertAt80Sent: Boolean = false,
    val alertAt100Sent: Boolean = false,
    val isExpired: Boolean = false
)
