package com.anestesia.app.presentation.patient

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "patient_prefs")

// Not annotated with @HiltViewModel — provided as a @Singleton via PatientModule
// so it can be injected into other ViewModels directly.
class PatientViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        val WEIGHT_KEY = doublePreferencesKey("patient_weight_kg")
    }

    private val _weightKg = MutableStateFlow(0.0)
    val weightKg: StateFlow<Double> = _weightKg.asStateFlow()

    init {
        viewModelScope.launch {
            context.dataStore.data.map { prefs ->
                prefs[WEIGHT_KEY] ?: 0.0
            }.collect { weight ->
                _weightKg.value = weight
            }
        }
    }

    fun saveWeight(weightKg: Double) {
        viewModelScope.launch {
            context.dataStore.edit { prefs ->
                prefs[WEIGHT_KEY] = weightKg
            }
            _weightKg.value = weightKg
        }
    }

    fun isWeightValid(): Boolean = _weightKg.value > 0.0
}
