package com.anestesia.app.presentation.main

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anestesia.app.data.repository.DrugRepository
import com.anestesia.app.data.repository.TimerRepository
import com.anestesia.app.domain.model.ActiveTimer
import com.anestesia.app.domain.model.Drug
import com.anestesia.app.presentation.patient.PatientViewModel
import com.anestesia.app.service.TimerForegroundService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainUiState(
    val activeTimers: List<ActiveTimer> = emptyList(),
    val drugs: List<Drug> = emptyList(),
    val patientWeightKg: Double = 0.0,
    val showWeightDialog: Boolean = false,
    val showAdministerDialog: Boolean = false,
    val showPanicDialog: Boolean = false,
    val selectedDrug: Drug? = null,
    val message: String? = null,
    val tick: Long = 0L // Used to force recomposition for timer countdowns
)

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val drugRepository: DrugRepository,
    private val timerRepository: TimerRepository,
    private val patientViewModel: PatientViewModel
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    val patientWeight: StateFlow<Double> = patientViewModel.weightKg

    init {
        observeData()
        startTickLoop()
    }

    private fun observeData() {
        viewModelScope.launch {
            drugRepository.getAllDrugsFlow().collect { drugs ->
                _uiState.update { it.copy(drugs = drugs) }
            }
        }
        viewModelScope.launch {
            timerRepository.getActiveTimersFlow().collect { timers ->
                _uiState.update { it.copy(activeTimers = timers.filter { t -> !t.isExpired }) }
            }
        }
        viewModelScope.launch {
            patientViewModel.weightKg.collect { weight ->
                _uiState.update { it.copy(patientWeightKg = weight) }
            }
        }
    }

    private fun startTickLoop() {
        viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                _uiState.update { it.copy(tick = System.currentTimeMillis()) }
            }
        }
    }

    // ── Patient Weight ────────────────────────────────────────────────────────

    fun showWeightDialog() {
        _uiState.update { it.copy(showWeightDialog = true) }
    }

    fun dismissWeightDialog() {
        _uiState.update { it.copy(showWeightDialog = false) }
    }

    fun savePatientWeight(weightKg: Double) {
        patientViewModel.saveWeight(weightKg)
        _uiState.update { it.copy(showWeightDialog = false) }
    }

    // ── Administer Drug ───────────────────────────────────────────────────────

    fun showAdministerDialog(drug: Drug) {
        if (!patientViewModel.isWeightValid()) {
            _uiState.update { it.copy(
                message = "⚠️ Configure el peso del paciente antes de administrar",
                showWeightDialog = true
            ) }
            return
        }
        _uiState.update { it.copy(showAdministerDialog = true, selectedDrug = drug) }
    }

    fun dismissAdministerDialog() {
        _uiState.update { it.copy(showAdministerDialog = false, selectedDrug = null) }
    }

    fun administerDrug(drug: Drug, weightKg: Double) {
        viewModelScope.launch {
            val volumeMl = drug.calculateVolumeMl(weightKg)
            val timer = ActiveTimer(
                drugId = drug.id,
                drugName = drug.name,
                drugCategory = drug.category,
                antidote = drug.antidote,
                administeredAtMs = System.currentTimeMillis(),
                reinjectionTimeMs = drug.reinjectionTimeMinutes * 60_000L,
                calculatedVolumeMl = volumeMl,
                patientWeightKg = weightKg
            )
            timerRepository.insertTimer(timer)
            TimerForegroundService.startService(context)
            _uiState.update {
                it.copy(
                    showAdministerDialog = false,
                    selectedDrug = null,
                    message = "✓ ${drug.name} administrado – ${String.format("%.2f", volumeMl)} ml"
                )
            }
        }
    }

    // ── Stop Timer ────────────────────────────────────────────────────────────

    fun stopTimer(timer: ActiveTimer) {
        viewModelScope.launch {
            timerRepository.deleteTimer(timer)
            val remaining = _uiState.value.activeTimers.filter { it.id != timer.id }
            if (remaining.isEmpty()) {
                TimerForegroundService.stopService(context)
            }
        }
    }

    // ── Panic Button ──────────────────────────────────────────────────────────

    fun showPanicDialog() {
        _uiState.update { it.copy(showPanicDialog = true) }
    }

    fun dismissPanicDialog() {
        _uiState.update { it.copy(showPanicDialog = false) }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun clearExpiredTimers() {
        viewModelScope.launch {
            timerRepository.clearExpiredTimers()
        }
    }

    /**
     * Returns active timers sorted by urgency (critical first, then warning).
     */
    fun getUrgentTimers(): List<ActiveTimer> {
        return _uiState.value.activeTimers.sortedByDescending { it.progressFraction }
    }
}
