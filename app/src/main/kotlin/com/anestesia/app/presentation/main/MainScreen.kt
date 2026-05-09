package com.anestesia.app.presentation.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.anestesia.app.domain.model.ActiveTimer
import com.anestesia.app.domain.model.Drug
import com.anestesia.app.presentation.theme.AstmColors
import com.anestesia.app.presentation.theme.drugCategoryColor
import java.util.concurrent.TimeUnit
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel(),
    onNavigateToVademecum: () -> Unit,
    onNavigateToAbout: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val patientWeight by viewModel.patientWeight.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Tick from ViewModel drives recomposition of all timers every second
    val tick = uiState.tick

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("AnesteFlow", fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp, color = AstmColors.OnSurface)
                        Text(
                            if (patientWeight > 0) "Paciente: ${patientWeight} kg"
                            else "⚠ Sin peso configurado",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (patientWeight > 0) AstmColors.Safe else AstmColors.Warning
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.showWeightDialog() }) {
                        Icon(Icons.Default.Person, contentDescription = "Peso paciente",
                            tint = AstmColors.OnSurface)
                    }
                    IconButton(onClick = onNavigateToVademecum) {
                        Icon(Icons.Default.MedicalServices, contentDescription = "Vademécum",
                            tint = AstmColors.OnSurface)
                    }
                    IconButton(onClick = onNavigateToAbout) {
                        Icon(Icons.Default.Info, contentDescription = "Acerca de",
                            tint = AstmColors.OnSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AstmColors.Surface)
            )
        },
        floatingActionButton = {
            // PANIC BUTTON
            FloatingActionButton(
                onClick = { viewModel.showPanicDialog() },
                containerColor = AstmColors.PanicRed,
                modifier = Modifier.size(72.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Warning, contentDescription = "Pánico",
                        tint = Color.White, modifier = Modifier.size(28.dp))
                    Text("PÁNICO", color = Color.White, fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold)
                }
            }
        },
        containerColor = AstmColors.Background
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = 96.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Active Timers Section
            if (uiState.activeTimers.isNotEmpty()) {
                item {
                    SectionHeader("⏱ TEMPORIZADORES ACTIVOS", uiState.activeTimers.size)
                }
                items(uiState.activeTimers, key = { it.id }) { timer ->
                    ActiveTimerCard(
                        timer = timer,
                        tick = tick,
                        onStop = { viewModel.stopTimer(timer) }
                    )
                }
                item {
                    TextButton(
                        onClick = { viewModel.clearExpiredTimers() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Limpiar finalizados")
                    }
                }
            } else {
                item {
                    EmptyTimersCard()
                }
            }

            // Drug List for administration
            item {
                SectionHeader("💊 ADMINISTRAR FÁRMACO", uiState.drugs.size)
            }

            val grouped = uiState.drugs.groupBy { it.category }
            grouped.forEach { (_, drugs) ->
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        items(drugs) { drug ->
                            DrugAdminButton(
                                drug = drug,
                                onClick = { viewModel.showAdministerDialog(drug) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Weight dialog
    if (uiState.showWeightDialog) {
        WeightDialog(
            currentWeight = patientWeight,
            onDismiss = { viewModel.dismissWeightDialog() },
            onSave = { viewModel.savePatientWeight(it) }
        )
    }

    // Administer dialog
    if (uiState.showAdministerDialog && uiState.selectedDrug != null) {
        AdministerDialog(
            drug = uiState.selectedDrug!!,
            weightKg = patientWeight,
            onDismiss = { viewModel.dismissAdministerDialog() },
            onConfirm = { drug, weight -> viewModel.administerDrug(drug, weight) }
        )
    }

    // Panic dialog
    if (uiState.showPanicDialog) {
        PanicDialog(
            activeTimers = viewModel.getUrgentTimers(),
            onDismiss = { viewModel.dismissPanicDialog() }
        )
    }
}

// ── Components ────────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold, color = AstmColors.OnSurfaceVariant,
            letterSpacing = 0.8.sp)
        Badge(containerColor = MaterialTheme.colorScheme.primary) {
            Text("$count")
        }
    }
}

@Composable
private fun ActiveTimerCard(
    timer: ActiveTimer,
    tick: Long,      // consumed here to force recomposition every second
    onStop: () -> Unit
) {
    // Recompute live from current wall clock — tick triggers recomposition
    val nowMs = tick.takeIf { it > 0L } ?: System.currentTimeMillis()
    val elapsedMs = nowMs - timer.administeredAtMs
    val remainingMs = maxOf(0L, timer.reinjectionTimeMs - elapsedMs)
    val progress = (elapsedMs.toFloat() / timer.reinjectionTimeMs).coerceIn(0f, 1f)
    val isCritical = progress >= 1.0f
    val isWarning = progress >= 0.80f && !isCritical

    val cardBorderColor by animateColorAsState(
        targetValue = when {
            isCritical -> AstmColors.Critical
            isWarning -> AstmColors.Warning
            else -> AstmColors.CardBorder
        },
        animationSpec = tween(300), label = "border"
    )
    val categoryColor = drugCategoryColor(timer.drugCategory)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCritical) Color(0xFFFFF3F3) else AstmColors.Surface
        ),
        border = BorderStroke(if (isCritical || isWarning) 2.dp else 1.dp, cardBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular countdown timer
            CircularTimer(
                progress = progress,
                remainingMs = remainingMs,
                color = when {
                    isCritical -> AstmColors.Critical
                    isWarning -> AstmColors.Warning
                    else -> categoryColor
                },
                size = 100
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(categoryColor, shape = MaterialTheme.shapes.small)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        timer.drugName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AstmColors.OnSurface
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "${String.format("%.2f", timer.calculatedVolumeMl)} ml administrado",
                    style = MaterialTheme.typography.bodySmall,
                    color = AstmColors.OnSurfaceVariant
                )
                }
                when {
                    isCritical -> Text(
                        "⚠️ REQUIERE REINYECCIÓN",
                        style = MaterialTheme.typography.labelMedium,
                        color = AstmColors.Critical, fontWeight = FontWeight.ExtraBold
                    )
                    isWarning -> Text(
                        "⚡ Ventana cerrando",
                        style = MaterialTheme.typography.labelMedium,
                        color = AstmColors.Warning, fontWeight = FontWeight.Bold
                    )
                }
            }
            IconButton(onClick = onStop, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.Stop, contentDescription = "Detener",
                    tint = AstmColors.Critical, modifier = Modifier.size(28.dp))
            }
        }
    }
}

@Composable
private fun CircularTimer(
    progress: Float,
    remainingMs: Long,
    color: Color,
    size: Int
) {
    val hours = TimeUnit.MILLISECONDS.toHours(remainingMs)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(remainingMs) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(remainingMs) % 60

    val timeText = if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(size.dp)
    ) {
        Canvas(modifier = Modifier.size(size.dp)) {
            val stroke = size * 0.1f
            val padding = stroke / 2
            val diameter = min(drawContext.size.width, drawContext.size.height) - stroke
            val topLeft = Offset(padding, padding)
            val arcSize = Size(diameter, diameter)

            // Track
            drawArc(
                color = color.copy(alpha = 0.15f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            // Progress
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        Text(
            text = timeText,
            style = MaterialTheme.typography.labelSmall,
            fontSize = if (hours > 0) 11.sp else 14.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DrugAdminButton(drug: Drug, onClick: () -> Unit) {
    val color = drugCategoryColor(drug.category)
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        modifier = Modifier.height(56.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(drug.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
            Text("${drug.doseMgKg} mg/kg", fontSize = 9.sp, color = Color.White.copy(alpha = 0.85f))
        }
    }
}

@Composable
private fun EmptyTimersCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AstmColors.Surface),
        border = BorderStroke(1.dp, AstmColors.CardBorder)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null,
                modifier = Modifier.size(48.dp), tint = AstmColors.Safe)
            Spacer(Modifier.height(8.dp))
            Text("Sin fármacos activos",
                style = MaterialTheme.typography.titleMedium,
                color = AstmColors.OnSurfaceVariant)
            Text("Seleccione un fármaco abajo para administrar",
                style = MaterialTheme.typography.bodySmall,
                color = AstmColors.Outline, textAlign = TextAlign.Center)
        }
    }
}

// ── Dialogs ───────────────────────────────────────────────────────────────────

@Composable
private fun WeightDialog(
    currentWeight: Double,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var weightText by remember { mutableStateOf(if (currentWeight > 0) currentWeight.toString() else "") }
    var error by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Person, contentDescription = null) },
        title = { Text("Peso del Paciente", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it; error = "" },
                    label = { Text("Peso (kg)*") },
                    isError = error.isNotBlank(),
                    supportingText = { if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "⚠ Este valor es crítico para el cálculo de dosis",
                    style = MaterialTheme.typography.bodySmall,
                    color = AstmColors.Warning,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val weight = weightText.toDoubleOrNull()
                when {
                    weight == null -> error = "Ingrese un valor numérico válido"
                    weight <= 0 -> error = "El peso debe ser mayor a 0"
                    weight > 300 -> error = "Valor fuera de rango clínico (>300 kg)"
                    else -> onSave(weight)
                }
            }) { Text("Confirmar") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun AdministerDialog(
    drug: Drug,
    weightKg: Double,
    onDismiss: () -> Unit,
    onConfirm: (Drug, Double) -> Unit
) {
    val volumeMl = drug.calculateVolumeMl(weightKg)
    val categoryColor = drugCategoryColor(drug.category)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(14.dp).background(categoryColor, MaterialTheme.shapes.small))
                Spacer(Modifier.width(8.dp))
                Text("Administrar ${drug.name}", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Dose calculation card
                Surface(
                    color = categoryColor.copy(alpha = 0.08f),
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, categoryColor.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("CÁLCULO DE DOSIS", style = MaterialTheme.typography.labelSmall,
                            color = categoryColor, fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp)
                        Spacer(Modifier.height(8.dp))
                        DoseRow("Peso paciente", "$weightKg kg")
                        DoseRow("Dosis", "${drug.doseMgKg} mg/kg")
                        DoseRow("Concentración", "${drug.concentrationMgMl} mg/ml")
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Row(horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()) {
                            Text("VOLUMEN A ADMINISTRAR",
                                fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                            Text(String.format("%.2f ml", volumeMl),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp, color = categoryColor)
                        }
                    }
                }
                Text("Iniciar cronómetro de ${drug.reinjectionTimeMinutes} minutos",
                    style = MaterialTheme.typography.bodySmall, color = AstmColors.OnSurfaceVariant)
                if (drug.antidote.isNotBlank() && drug.antidote != "N/A") {
                    Surface(color = AstmColors.Safe.copy(alpha = 0.08f),
                        shape = MaterialTheme.shapes.small) {
                        Text("💊 Antídoto disponible: ${drug.antidote}",
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = AstmColors.Safe, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(drug, weightKg) },
                colors = ButtonDefaults.buttonColors(containerColor = categoryColor)
            ) { Text("✓ Administrar", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun DoseRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = AstmColors.OnSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold,
            color = AstmColors.OnSurface)
    }
}

@Composable
private fun PanicDialog(
    activeTimers: List<ActiveTimer>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.Warning, contentDescription = null,
                tint = AstmColors.PanicRed, modifier = Modifier.size(40.dp))
        },
        title = {
            Text("INTERVENCIÓN DE EMERGENCIA",
                fontWeight = FontWeight.ExtraBold,
                color = AstmColors.PanicRed,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth())
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (activeTimers.isEmpty()) {
                    Text("Sin fármacos activos en este momento.",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        color = AstmColors.OnSurfaceVariant)
                } else {
                    Text("ANTÍDOTOS PRIORITARIOS:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp)
                    activeTimers.forEach { timer ->
                        AntidoteCard(timer)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = AstmColors.PanicRed),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("CERRAR", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            }
        }
    )
}

@Composable
private fun AntidoteCard(timer: ActiveTimer) {
    val categoryColor = drugCategoryColor(timer.drugCategory)
    val urgencyLabel = when {
        timer.isCritical -> "⚠️ CRÍTICO"
        timer.isWarning -> "⚡ ALERTA"
        else -> "ACTIVO"
    }
    val urgencyColor = when {
        timer.isCritical -> AstmColors.Critical
        timer.isWarning -> AstmColors.Warning
        else -> AstmColors.Safe
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = AstmColors.Surface),
        border = BorderStroke(2.dp, urgencyColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(12.dp).background(categoryColor, MaterialTheme.shapes.small))
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(timer.drugName, fontWeight = FontWeight.Bold, color = AstmColors.OnSurface)
                if (timer.antidote.isNotBlank() && timer.antidote != "N/A") {
                    Text("💊 ${timer.antidote}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AstmColors.Safe, fontWeight = FontWeight.SemiBold)
                } else {
                    Text("Sin antídoto específico",
                        style = MaterialTheme.typography.bodySmall,
                        color = AstmColors.OnSurfaceVariant)
                }
            }
            Badge(containerColor = urgencyColor) {
                Text(urgencyLabel, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}
