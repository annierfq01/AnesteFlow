package com.anestesia.app.data.repository

import android.content.Context
import android.net.Uri
import com.anestesia.app.data.local.dao.ActiveTimerDao
import com.anestesia.app.data.local.dao.DrugDao
import com.anestesia.app.data.local.entity.*
import com.anestesia.app.domain.model.ActiveTimer
import com.anestesia.app.domain.model.Drug
import com.anestesia.app.domain.model.VademecumBackup
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

interface DrugRepository {
    fun getAllDrugsFlow(): Flow<List<Drug>>
    suspend fun getAllDrugs(): List<Drug>
    suspend fun insertDrug(drug: Drug): Long
    suspend fun updateDrug(drug: Drug)
    suspend fun deleteDrug(drug: Drug)
    suspend fun upsertAll(drugs: List<Drug>)
}

interface TimerRepository {
    fun getActiveTimersFlow(): Flow<List<ActiveTimer>>
    suspend fun getActiveTimers(): List<ActiveTimer>
    suspend fun insertTimer(timer: ActiveTimer): Long
    suspend fun updateTimer(timer: ActiveTimer)
    suspend fun markAlert80Sent(id: Int)
    suspend fun markAlert100Sent(id: Int)
    suspend fun markExpired(id: Int)
    suspend fun deleteTimer(timer: ActiveTimer)
    suspend fun clearExpiredTimers()
}

interface BackupRepository {
    suspend fun exportVademecum(drugs: List<Drug>, uri: Uri)
    suspend fun importVademecum(uri: Uri): Result<VademecumBackup>
}

// ── Implementations ──────────────────────────────────────────────────────────

@Singleton
class DrugRepositoryImpl @Inject constructor(
    private val drugDao: DrugDao
) : DrugRepository {

    override fun getAllDrugsFlow(): Flow<List<Drug>> =
        drugDao.getAllDrugsFlow().map { list -> list.map { it.toDomain() } }

    override suspend fun getAllDrugs(): List<Drug> =
        drugDao.getAllDrugs().map { it.toDomain() }

    override suspend fun insertDrug(drug: Drug): Long =
        drugDao.insertDrug(drug.toEntity())

    override suspend fun updateDrug(drug: Drug) =
        drugDao.updateDrug(drug.toEntity())

    override suspend fun deleteDrug(drug: Drug) =
        drugDao.softDeleteDrug(drug.id)

    override suspend fun upsertAll(drugs: List<Drug>) =
        drugDao.upsertAll(drugs.map { it.toEntity() })
}

@Singleton
class TimerRepositoryImpl @Inject constructor(
    private val timerDao: ActiveTimerDao
) : TimerRepository {

    override fun getActiveTimersFlow(): Flow<List<ActiveTimer>> =
        timerDao.getActiveTimersFlow().map { list -> list.map { it.toDomain() } }

    override suspend fun getActiveTimers(): List<ActiveTimer> =
        timerDao.getActiveTimers().map { it.toDomain() }

    override suspend fun insertTimer(timer: ActiveTimer): Long =
        timerDao.insertTimer(timer.toEntity())

    override suspend fun updateTimer(timer: ActiveTimer) =
        timerDao.updateTimer(timer.toEntity())

    override suspend fun markAlert80Sent(id: Int) = timerDao.markAlert80Sent(id)
    override suspend fun markAlert100Sent(id: Int) = timerDao.markAlert100Sent(id)
    override suspend fun markExpired(id: Int) = timerDao.markExpired(id)

    override suspend fun deleteTimer(timer: ActiveTimer) =
        timerDao.deleteTimer(timer.toEntity())

    override suspend fun clearExpiredTimers() = timerDao.clearExpiredTimers()
}

@Singleton
class BackupRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : BackupRepository {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    override suspend fun exportVademecum(drugs: List<Drug>, uri: Uri) {
        val backup = VademecumBackup(drugs = drugs)
        val jsonString = json.encodeToString(backup)
        context.contentResolver.openOutputStream(uri)?.use { stream ->
            stream.write(jsonString.toByteArray(Charsets.UTF_8))
        } ?: throw IllegalStateException("No se pudo abrir el archivo de destino")
    }

    override suspend fun importVademecum(uri: Uri): Result<VademecumBackup> = runCatching {
        val jsonString = context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.readBytes().toString(Charsets.UTF_8)
        } ?: throw IllegalStateException("No se pudo leer el archivo")
        json.decodeFromString<VademecumBackup>(jsonString)
    }
}
