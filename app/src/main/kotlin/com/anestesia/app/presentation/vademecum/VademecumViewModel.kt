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

    /**
     * Vademécum predeterminado basado en guías clínicas vigentes (ASA 2024, ESAIC 2023).
     * Dosis de inducción estándar en adultos. Ajustar siempre según ASA, edad y comorbilidades.
     *
     * OPIOIDES
     * - Fentanilo: 2-5 mcg/kg inducción. Duración 30-60 min. Antídoto: Naloxona.
     * - Morfina: 0.1-0.2 mg/kg. Duración 4-6 h. Antídoto: Naloxona.
     * - Remifentanilo: 1 mcg/kg bolo / infusión 0.1-0.5 mcg/kg/min. Ultra-corto. Antídoto: Naloxona.
     *
     * HIPNÓTICOS
     * - Propofol: 1.5-2.5 mg/kg inducción, 10 mg/ml. Duración 10 min. Sin antídoto específico.
     * - Ketamina: 1-2 mg/kg IV inducción. Duración 15 min. Sin antídoto (BZD para delirio).
     * - Etomidato: 0.3 mg/kg IV. Duración 5 min. Elección en inestabilidad hemodinámica.
     * - Tiopental: 3-5 mg/kg IV. Duración 10-15 min. Histórico, uso decreciente.
     *
     * RELAJANTES MUSCULARES
     * - Succinilcolina: 1-1.5 mg/kg ISR. Duración 10 min. SIN antídoto específico.
     *   NOTA: Sugammadex NO revierte succinilcolina (despolarizante).
     * - Rocuronio: 0.6 mg/kg (1.2 mg/kg ISR). Duración 30-60 min. Antídoto: Sugammadex.
     * - Vecuronio: 0.1 mg/kg. Duración 25-40 min. Antídoto: Sugammadex / Neostigmina+Atropina.
     * - Cisatracurio: 0.15 mg/kg. Duración 40-75 min. Elección en fallo renal/hepático.
     *   Antídoto: Neostigmina+Atropina (Sugammadex NO lo revierte).
     *
     * BENZODIACEPINAS
     * - Midazolam: 0.02-0.05 mg/kg premedicación. Duración 20-80 min. Antídoto: Flumazenil.
     * - Diazepam: 0.1-0.2 mg/kg. Duración hasta 12 h. Antídoto: Flumazenil.
     *
     * ALFA-2 AGONISTAS (adyuvantes)
     * - Dexmedetomidina: bolo 1 mcg/kg/10 min, mantenimiento 0.2-1 mcg/kg/h.
     *   Sedación cooperativa, no deprime respiración. Antídoto: Atipamezol (uso veterinario,
     *   off-label en humanos) / manejo sintomático (atropina para bradicardia).
     *
     * ANESTÉSICOS LOCALES
     * - Lidocaína: 1-1.5 mg/kg IV adyuvante. Antídoto: Intralipid 20%.
     * - Bupivacaína: 0.5-2 mg/kg bloqueo regional. Larga duración 4-8 h. Antídoto: Intralipid 20%.
     */
    private fun seedDefaultDrugsIfEmpty() {
        viewModelScope.launch {
            val existing = drugRepository.getAllDrugs()
            if (existing.isNotEmpty()) return@launch

            val defaults = listOf(

                // ── OPIOIDES ──────────────────────────────────────────────────
                Drug(
                    name = "Fentanilo",
                    category = DrugCategory.OPIOIDE.name,
                    doseMgKg = 0.003,          // 3 mcg/kg = 0.003 mg/kg (dosis media inducción)
                    concentrationMgMl = 0.05,  // 50 mcg/ml = 0.05 mg/ml (ampolla estándar)
                    reinjectionTimeMinutes = 30,
                    antidote = "Naloxona 0.4 mg IV (repetir cada 2-3 min hasta 10 mg)",
                    notes = "Inducción: 2-5 mcg/kg. Mantenimiento: 1-3 mcg/kg/h infusión. " +
                            "Riesgo rigidez torácica a dosis altas. Ampolla 0.05 mg/ml (50 mcg/ml)."
                ),
                Drug(
                    name = "Morfina",
                    category = DrugCategory.OPIOIDE.name,
                    doseMgKg = 0.1,
                    concentrationMgMl = 1.0,   // 10 mg/ml ampolla estándar
                    reinjectionTimeMinutes = 240,
                    antidote = "Naloxona 0.4 mg IV (repetir cada 2-3 min hasta 10 mg)",
                    notes = "Analgesia intra/postoperatoria. Inicio: 5-10 min IV. " +
                            "Duración: 4-6 h. Precaución en insuficiencia renal (acumulación M6G)."
                ),
                Drug(
                    name = "Remifentanilo",
                    category = DrugCategory.OPIOIDE.name,
                    doseMgKg = 0.001,          // 1 mcg/kg bolo
                    concentrationMgMl = 0.05,  // Preparar a 50 mcg/ml para infusión
                    reinjectionTimeMinutes = 5, // Vida media contexto-sensible ~3-5 min
                    antidote = "Naloxona 0.4 mg IV",
                    notes = "Ultra-corto acción. Infusión: 0.1-0.5 mcg/kg/min. Metabolismo " +
                            "esterásico plasmático (independiente de función hepática/renal). " +
                            "Requiere analgesia anticipada al cierre (morfina/AINEs)."
                ),
                Drug(
                    name = "Tramadol",
                    category = DrugCategory.OPIOIDE.name,
                    doseMgKg = 1.5,
                    concentrationMgMl = 50.0,  // 50 mg/ml
                    reinjectionTimeMinutes = 360,
                    antidote = "Naloxona 0.4 mg IV (efecto parcial)",
                    notes = "Analgesia postoperatoria leve-moderada. Dosis: 1-2 mg/kg IV lento. " +
                            "Duración 4-6 h. Riesgo convulsiones con ISRS/IMAO. Diluir en 100 ml SF."
                ),

                // ── HIPNÓTICOS ────────────────────────────────────────────────
                Drug(
                    name = "Propofol",
                    category = DrugCategory.HIPNOTICO.name,
                    doseMgKg = 2.0,
                    concentrationMgMl = 10.0,  // 10 mg/ml ampolla estándar
                    reinjectionTimeMinutes = 10,
                    antidote = "No existe antídoto. Soporte hemodinámico (efedrina/fenilefrina).",
                    notes = "Inducción: 1.5-2.5 mg/kg IV lento. Mantenimiento: 4-12 mg/kg/h. " +
                            "Reducir dosis en ancianos/ASA III-IV. Dolor en inyección (lidocaína previa). " +
                            "Síndrome por infusión de propofol (PRIS) con infusiones >48h a dosis altas."
                ),
                Drug(
                    name = "Ketamina",
                    category = DrugCategory.HIPNOTICO.name,
                    doseMgKg = 1.5,
                    concentrationMgMl = 10.0,  // 10 mg/ml o 50 mg/ml según presentación
                    reinjectionTimeMinutes = 15,
                    antidote = "No específico. Benzodiacepinas (midazolam) para delirio/alucinaciones.",
                    notes = "Inducción: 1-2 mg/kg IV / 4-6 mg/kg IM. Anestésico disociativo. " +
                            "Cardioestimulador — IDEAL en inestabilidad hemodinámica y asma. " +
                            "Aumenta secreciones: premedicar con atropina. Contraindicado en HTA " +
                            "severa, HIC, glaucoma, psicosis activa."
                ),
                Drug(
                    name = "Etomidato",
                    category = DrugCategory.HIPNOTICO.name,
                    doseMgKg = 0.3,
                    concentrationMgMl = 2.0,   // 2 mg/ml ampolla estándar
                    reinjectionTimeMinutes = 5,
                    antidote = "No existe antídoto. Soporte hemodinámico.",
                    notes = "Inducción: 0.15-0.3 mg/kg IV. Duración: 5-10 min. " +
                            "MENOR repercusión hemodinámica de todos los inductores — " +
                            "ELECCIÓN en politraumatismo, cardiopatía severa, shock. " +
                            "Suprime cortisol adrenal hasta 6-8 h post-dosis única. " +
                            "Evitar infusión continua (insuficiencia suprarrenal)."
                ),

                // ── RELAJANTES MUSCULARES ─────────────────────────────────────
                Drug(
                    name = "Succinilcolina",
                    category = DrugCategory.RELAJANTE.name,
                    doseMgKg = 1.5,
                    concentrationMgMl = 20.0,  // 20 mg/ml
                    reinjectionTimeMinutes = 10,
                    antidote = "⚠️ SIN antídoto específico. Sugammadex NO revierte succinilcolina " +
                               "(es despolarizante). Ventilación de soporte hasta recuperación espontánea.",
                    notes = "ISR: 1-1.5 mg/kg IV / 3-4 mg/kg IM. Inicio: 30-60 s. Duración: 8-10 min. " +
                            "CONTRAINDICADO: quemados, aplastamiento, miopatías, hiperpotasemia, " +
                            "hipertermia maligna personal/familiar. Elección ISR cuando Rocuronio no disponible."
                ),
                Drug(
                    name = "Rocuronio",
                    category = DrugCategory.RELAJANTE.name,
                    doseMgKg = 0.6,
                    concentrationMgMl = 10.0,  // 10 mg/ml
                    reinjectionTimeMinutes = 30,
                    antidote = "Sugammadex: 2 mg/kg (TOF≥2) / 4 mg/kg (TOF<2) / " +
                               "16 mg/kg EMERGENCIA (revertir inmediatamente tras inducción).",
                    notes = "Inducción estándar: 0.6 mg/kg. ISR alternativa a Succinilcolina: 1.2 mg/kg. " +
                            "Duración: 30-60 min (0.6) / 60-90 min (1.2). Ampolla 10 mg/ml. " +
                            "Refrigerar. Antídoto Sugammadex SIEMPRE disponible si se usa a 1.2 mg/kg."
                ),
                Drug(
                    name = "Vecuronio",
                    category = DrugCategory.RELAJANTE.name,
                    doseMgKg = 0.1,
                    concentrationMgMl = 1.0,   // 1 mg/ml reconstituido
                    reinjectionTimeMinutes = 25,
                    antidote = "Sugammadex: 2-4 mg/kg según profundidad bloqueo. " +
                               "Alternativa: Neostigmina 0.05 mg/kg + Atropina 0.02 mg/kg (bloqueo superficial).",
                    notes = "Intubación: 0.1 mg/kg. Mantenimiento: 0.01-0.015 mg/kg. " +
                            "Duración: 25-40 min. Liofilizado — reconstituir antes de uso. " +
                            "Eliminación biliar/renal. Precaución en hepatopatía."
                ),
                Drug(
                    name = "Cisatracurio",
                    category = DrugCategory.RELAJANTE.name,
                    doseMgKg = 0.15,
                    concentrationMgMl = 2.0,   // 2 mg/ml
                    reinjectionTimeMinutes = 45,
                    antidote = "Neostigmina 0.05 mg/kg + Atropina 0.02 mg/kg IV. " +
                               "⚠️ Sugammadex NO revierte cisatracurio (no es esteroideo).",
                    notes = "Inducción: 0.15 mg/kg. Mantenimiento: 0.03 mg/kg c/20 min. " +
                            "Duración: 40-75 min. Degradación de Hofmann (independiente hepático/renal). " +
                            "ELECCIÓN en insuficiencia renal, hepática o fallo multiorgánico. " +
                            "Sin liberación de histamina significativa."
                ),

                // ── BENZODIACEPINAS ───────────────────────────────────────────
                Drug(
                    name = "Midazolam",
                    category = DrugCategory.BENZODIACEPINA.name,
                    doseMgKg = 0.03,
                    concentrationMgMl = 1.0,   // 1 mg/ml o 5 mg/ml
                    reinjectionTimeMinutes = 60,
                    antidote = "Flumazenil 0.2 mg IV cada 60 s hasta 1 mg total. " +
                               "⚠️ Vida media corta (1 h): puede necesitar re-dosificación.",
                    notes = "Premedicación: 0.02-0.05 mg/kg IV. Sedación: 0.02-0.1 mg/kg. " +
                            "Inicio: 1-2 min IV. Duración: 20-80 min. Potencia amnesia anterógrada. " +
                            "Reducir dosis 30-50% en ancianos y en combinación con opioides."
                ),
                Drug(
                    name = "Diazepam",
                    category = DrugCategory.BENZODIACEPINA.name,
                    doseMgKg = 0.15,
                    concentrationMgMl = 5.0,   // 5 mg/ml
                    reinjectionTimeMinutes = 480, // 8 horas (vida media larga)
                    antidote = "Flumazenil 0.2 mg IV cada 60 s hasta 1 mg. " +
                               "⚠️ Vida media 20-100 h: riesgo resedación tras flumazenil.",
                    notes = "Premedicación VO: 0.1-0.2 mg/kg. IV lento: dolor en vena. " +
                            "Duración larga (12-24 h efecto clínico). Metabolito activo (nordiazepam). " +
                            "Evitar en ancianos (acumulación). Preferir midazolam en anestesia."
                ),

                // ── ADYUVANTES / ALFA-2 AGONISTAS ────────────────────────────
                Drug(
                    name = "Dexmedetomidina",
                    category = DrugCategory.OTRO.name,
                    doseMgKg = 0.001,          // 1 mcg/kg bolo de carga (= 0.001 mg/kg)
                    concentrationMgMl = 0.1,   // 100 mcg/ml diluido = 0.1 mg/ml
                    reinjectionTimeMinutes = 60, // Vida media 2 h, efecto 60-90 min
                    antidote = "No hay antídoto aprobado en humanos. Atropina 0.5-1 mg IV para " +
                               "bradicardia/hipotensión. Efedrina 5-10 mg IV si hipotensión severa.",
                    notes = "Bolo carga: 1 mcg/kg en 10 min (nunca bolo rápido — riesgo bradicardia). " +
                            "Mantenimiento: 0.2-1 mcg/kg/h. Sedación cooperativa — paciente responde " +
                            "a estímulos verbales. No deprime respiración. Usos: sedación UCI, " +
                            "analgesia adyuvante, intubación fibroscópica despierto, prevención " +
                            "delirio postoperatorio. Ampolla 200 mcg/2 ml. Diluir a 100 mcg/ml."
                ),

                // ── ANESTÉSICOS LOCALES ───────────────────────────────────────
                Drug(
                    name = "Lidocaína",
                    category = DrugCategory.OTRO.name,
                    doseMgKg = 1.5,
                    concentrationMgMl = 10.0,  // 10 mg/ml (1%)
                    reinjectionTimeMinutes = 60,
                    antidote = "Toxicidad sistémica (LAST): Intralipid 20% — bolo 1.5 ml/kg IV, " +
                               "luego infusión 0.25 ml/kg/min. Soporte ABC. Evitar lidocaína " +
                               "para tratar arritmias en toxicidad por lidocaína.",
                    notes = "IV adyuvante analgésico: 1-1.5 mg/kg bolo pre-inducción. " +
                            "Bloqueo regional infiltración: 4-5 mg/kg (sin adrenalina) / " +
                            "7 mg/kg (con adrenalina). Inicio: 1-2 min. Duración: 60-120 min. " +
                            "Dosis máxima adulto sin adrenalina: 4.5 mg/kg (300 mg)."
                ),
                Drug(
                    name = "Bupivacaína",
                    category = DrugCategory.OTRO.name,
                    doseMgKg = 2.0,
                    concentrationMgMl = 5.0,   // 5 mg/ml (0.5%)
                    reinjectionTimeMinutes = 300, // 4-8 horas
                    antidote = "Toxicidad sistémica (LAST): Intralipid 20% — bolo 1.5 ml/kg IV URGENTE, " +
                               "luego 0.25 ml/kg/min. RCP prolongada. Evitar lidocaína IV.",
                    notes = "Analgesia epidural: 0.25-0.5%. Bloqueo espinal: 0.5% hiperbárica. " +
                            "Bloqueo periférico: 0.25-0.5%. Duración: 4-8 h (con adrenalina hasta 12 h). " +
                            "ALTA cardiotoxicidad — dosis máx: 2.5 mg/kg sin adrenalina. " +
                            "Aspirar antes de inyectar. Disponible levobupivacaína (menor cardiotoxicidad)."
                )
            )
            defaults.forEach { drugRepository.insertDrug(it) }
        }
    }
}
