package com.anestesia.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad Room que representa un fármaco en el vademécum.
 * Categorías siguen el código de colores ASTM para anestesiología.
 */
@Entity(tableName = "drugs")
data class DrugEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val category: String,          // RELAJANTE, OPIOIDE, HIPNOTICO, BENZODIACEPINA, OTRO
    val doseMgKg: Double,          // Dosis en mg/kg
    val concentrationMgMl: Double, // Concentración mg/ml
    val reinjectionTimeMinutes: Int,// Tiempo de reinyección en minutos
    val antidote: String,          // Nombre del antídoto (puede estar vacío)
    val notes: String = "",
    val isActive: Boolean = true
)
