package com.anestesia.app.presentation.vademecum

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anestesia.app.data.repository.BackupRepository
import com.anestesia.app.data.repository.DrugRepository
import com.anestesia.app.domain.model.Drug
import com.anestesia.app.domain.model.DrugCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VademecumUiState(
    val drugs: List<Drug> = emptyList(),
    val isLoading: Boolean = false,
    val message: String? = null,
    val showDeleteDialog: Boolean = false,
    val drugToDelete: Drug? = null,
    val showAddEditDialog: Boolean = false,
    val editingDrug: Drug? = null,
)

@HiltViewModel
class VademecumViewModel @Inject constructor(
    private val drugRepository: DrugRepository,
    private val backupRepository: BackupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VademecumUiState())
    val uiState: StateFlow<VademecumUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            drugRepository.getAllDrugsFlow().collect { drugs ->
                _uiState.update { it.copy(drugs = drugs, isLoading = false) }
            }
        }
        seedDefaultDrugsIfEmpty()
    }

    fun saveDrug(drug: Drug) {
        viewModelScope.launch {
            if (drug.id == 0) {
                drugRepository.insertDrug(drug)
                _uiState.update { it.copy(message = "Fármaco '${drug.name}' agregado", showAddEditDialog = false, editingDrug = null) }
            } else {
                drugRepository.updateDrug(drug)
                _uiState.update { it.copy(message = "Fármaco '${drug.name}' actualizado", showAddEditDialog = false, editingDrug = null) }
            }
        }
    }

    fun requestDelete(drug: Drug) {
        _uiState.update { it.copy(showDeleteDialog = true, drugToDelete = drug) }
    }

    fun confirmDelete() {
        val drug = _uiState.value.drugToDelete ?: return
        viewModelScope.launch {
            drugRepository.deleteDrug(drug)
            _uiState.update {
                it.copy(showDeleteDialog = false, drugToDelete = null,
                    message = "Fármaco '${drug.name}' eliminado")
            }
        }
    }

    fun cancelDelete() {
        _uiState.update { it.copy(showDeleteDialog = false, drugToDelete = null) }
    }

    fun openAddDialog() {
        _uiState.update { it.copy(showAddEditDialog = true, editingDrug = null) }
    }

    fun openEditDialog(drug: Drug) {
        _uiState.update { it.copy(showAddEditDialog = true, editingDrug = drug) }
    }

    fun closeDialog() {
        _uiState.update { it.copy(showAddEditDialog = false, editingDrug = null) }
    }

    fun exportVademecum(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching {
                val drugs = drugRepository.getAllDrugs()
                backupRepository.exportVademecum(drugs, uri)
            }.onSuccess {
                _uiState.update { it.copy(isLoading = false, message = "Vademécum exportado exitosamente") }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, message = "Error al exportar: ${e.message}") }
            }
        }
    }

    fun importVademecum(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            backupRepository.importVademecum(uri)
                .onSuccess { backup ->
                    drugRepository.upsertAll(backup.drugs)
                    _uiState.update {
                        it.copy(isLoading = false,
                            message = "Importados ${backup.drugs.size} fármaco(s) correctamente")
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, message = "Error al importar: ${e.message}") }
                }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun seedDefaultDrugsIfEmpty() {
        viewModelScope.launch {
            val existing = drugRepository.getAllDrugs()
            if (existing.isNotEmpty()) return@launch

            val defaults = listOf(
                Drug(name = "Fentanilo", category = DrugCategory.OPIOIDE.name,
                    doseMgKg = 0.002, concentrationMgMl = 0.05,
                    reinjectionTimeMinutes = 30, antidote = "Naloxona",
                    notes = "Dosis: 2 mcg/kg. Concentración: 50 mcg/ml"),
                Drug(name = "Propofol", category = DrugCategory.HIPNOTICO.name,
                    doseMgKg = 2.0, concentrationMgMl = 10.0,
                    reinjectionTimeMinutes = 10, antidote = "N/A (soporte hemodinámico)",
                    notes = "Inducción: 1.5-2.5 mg/kg"),
                Drug(name = "Succinilcolina", category = DrugCategory.RELAJANTE.name,
                    doseMgKg = 1.5, concentrationMgMl = 20.0,
                    reinjectionTimeMinutes = 10, antidote = "Sugammadex (no específico)",
                    notes = "Acción ultracorta. IM: 3-4 mg/kg"),
                Drug(name = "Rocuronio", category = DrugCategory.RELAJANTE.name,
                    doseMgKg = 0.6, concentrationMgMl = 10.0,
                    reinjectionTimeMinutes = 30, antidote = "Sugammadex",
                    notes = "Reversión: Sugammadex 16 mg/kg dosis emergencia"),
                Drug(name = "Midazolam", category = DrugCategory.BENZODIACEPINA.name,
                    doseMgKg = 0.05, concentrationMgMl = 1.0,
                    reinjectionTimeMinutes = 60, antidote = "Flumazenil",
                    notes = "Premedicación: 0.02-0.05 mg/kg"),
                Drug(name = "Ketamina", category = DrugCategory.HIPNOTICO.name,
                    doseMgKg = 1.5, concentrationMgMl = 10.0,
                    reinjectionTimeMinutes = 15, antidote = "N/A (benzodiacepinas para delirio)",
                    notes = "Anestesia disociativa. No deprimir respiración"),
                Drug(name = "Morfina", category = DrugCategory.OPIOIDE.name,
                    doseMgKg = 0.1, concentrationMgMl = 1.0,
                    reinjectionTimeMinutes = 240, antidote = "Naloxona",
                    notes = "Analgesia postoperatoria"),
                Drug(name = "Vecuronio", category = DrugCategory.RELAJANTE.name,
                    doseMgKg = 0.1, concentrationMgMl = 1.0,
                    reinjectionTimeMinutes = 25, antidote = "Sugammadex / Neostigmina",
                    notes = "Intubación: 0.1 mg/kg"),
            )
            defaults.forEach { drugRepository.insertDrug(it) }
        }
    }
}
