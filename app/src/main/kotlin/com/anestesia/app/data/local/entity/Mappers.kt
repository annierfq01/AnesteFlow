package com.anestesia.app.data.local.entity

import com.anestesia.app.domain.model.ActiveTimer
import com.anestesia.app.domain.model.Drug

fun DrugEntity.toDomain(): Drug = Drug(
    id = id,
    name = name,
    category = category,
    doseMgKg = doseMgKg,
    concentrationMgMl = concentrationMgMl,
    reinjectionTimeMinutes = reinjectionTimeMinutes,
    antidote = antidote,
    notes = notes,
    isActive = isActive
)

fun Drug.toEntity(): DrugEntity = DrugEntity(
    id = id,
    name = name,
    category = category,
    doseMgKg = doseMgKg,
    concentrationMgMl = concentrationMgMl,
    reinjectionTimeMinutes = reinjectionTimeMinutes,
    antidote = antidote,
    notes = notes,
    isActive = isActive
)

fun ActiveTimerEntity.toDomain(): ActiveTimer = ActiveTimer(
    id = id,
    drugId = drugId,
    drugName = drugName,
    drugCategory = drugCategory,
    antidote = antidote,
    administeredAtMs = administeredAtMs,
    reinjectionTimeMs = reinjectionTimeMs,
    calculatedVolumeMl = calculatedVolumeMl,
    patientWeightKg = patientWeightKg,
    alertAt80Sent = alertAt80Sent,
    alertAt100Sent = alertAt100Sent,
    isExpired = isExpired
)

fun ActiveTimer.toEntity(): ActiveTimerEntity = ActiveTimerEntity(
    id = id,
    drugId = drugId,
    drugName = drugName,
    drugCategory = drugCategory,
    antidote = antidote,
    administeredAtMs = administeredAtMs,
    reinjectionTimeMs = reinjectionTimeMs,
    calculatedVolumeMl = calculatedVolumeMl,
    patientWeightKg = patientWeightKg,
    alertAt80Sent = alertAt80Sent,
    alertAt100Sent = alertAt100Sent,
    isExpired = isExpired
)
