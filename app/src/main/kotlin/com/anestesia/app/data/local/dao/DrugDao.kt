package com.anestesia.app.data.local.dao

import androidx.room.*
import com.anestesia.app.data.local.entity.DrugEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DrugDao {

    @Query("SELECT * FROM drugs WHERE isActive = 1 ORDER BY category, name")
    fun getAllDrugsFlow(): Flow<List<DrugEntity>>

    @Query("SELECT * FROM drugs WHERE isActive = 1 ORDER BY category, name")
    suspend fun getAllDrugs(): List<DrugEntity>

    @Query("SELECT * FROM drugs WHERE id = :id")
    suspend fun getDrugById(id: Int): DrugEntity?

    @Query("SELECT * FROM drugs WHERE name = :name LIMIT 1")
    suspend fun getDrugByName(name: String): DrugEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDrug(drug: DrugEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllDrugs(drugs: List<DrugEntity>): List<Long>

    @Update
    suspend fun updateDrug(drug: DrugEntity)

    /**
     * Upsert: actualiza si existe por nombre, inserta si no.
     * Usado para importación de JSON.
     */
    @Transaction
    suspend fun upsertDrug(drug: DrugEntity) {
        val existing = getDrugByName(drug.name)
        if (existing != null) {
            updateDrug(drug.copy(id = existing.id))
        } else {
            insertDrug(drug)
        }
    }

    @Transaction
    suspend fun upsertAll(drugs: List<DrugEntity>) {
        drugs.forEach { upsertDrug(it) }
    }

    @Query("UPDATE drugs SET isActive = 0 WHERE id = :id")
    suspend fun softDeleteDrug(id: Int)

    @Delete
    suspend fun hardDeleteDrug(drug: DrugEntity)

    @Query("DELETE FROM drugs")
    suspend fun deleteAll()
}
