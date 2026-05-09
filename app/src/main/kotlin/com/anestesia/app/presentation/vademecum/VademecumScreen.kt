package com.anestesia.app.presentation.vademecum

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.anestesia.app.domain.model.Drug
import com.anestesia.app.domain.model.DrugCategory
import com.anestesia.app.presentation.theme.AstmColors
import com.anestesia.app.presentation.theme.drugCategoryColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VademecumScreen(
    viewModel: VademecumViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // File pickers
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? -> uri?.let { viewModel.exportVademecum(it) } }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> uri?.let { viewModel.importVademecum(it) } }

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
                    Text("Vademécum", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                },
                actions = {
                    IconButton(onClick = { exportLauncher.launch("anestesia_backup.json") }) {
                        Icon(Icons.Default.Upload, contentDescription = "Exportar JSON")
                    }
                    IconButton(onClick = { importLauncher.launch(arrayOf("application/json")) }) {
                        Icon(Icons.Default.Download, contentDescription = "Importar JSON")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AstmColors.Surface,
                    titleContentColor = AstmColors.OnSurface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text("Nuevo Fármaco") },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                onClick = { viewModel.openAddDialog() },
                containerColor = MaterialTheme.colorScheme.primary
            )
        },
        containerColor = AstmColors.Background
    ) { paddingValues ->

        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp,
                    top = paddingValues.calculateTopPadding() + 8.dp,
                    bottom = 80.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Group by category
                val grouped = uiState.drugs.groupBy { it.category }
                DrugCategory.entries.forEach { cat ->
                    val drugs = grouped[cat.name] ?: return@forEach
                    item {
                        CategoryHeader(category = cat)
                    }
                    items(drugs, key = { it.id }) { drug ->
                        DrugCard(
                            drug = drug,
                            onEdit = { viewModel.openEditDialog(drug) },
                            onDelete = { viewModel.requestDelete(drug) }
                        )
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (uiState.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelDelete() },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFB71C1C)) },
            title = { Text("¿Eliminar fármaco?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Esta acción eliminará '${uiState.drugToDelete?.name}' del vademécum. ¿Desea continuar?")
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmDelete() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C))
                ) { Text("Eliminar") }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.cancelDelete() }) { Text("Cancelar") }
            }
        )
    }

    // Add/Edit dialog
    if (uiState.showAddEditDialog) {
        DrugEditDialog(
            drug = uiState.editingDrug,
            onDismiss = { viewModel.closeDialog() },
            onSave = { viewModel.saveDrug(it) }
        )
    }
}

@Composable
private fun CategoryHeader(category: DrugCategory) {
    val color = drugCategoryColor(category.name)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(6.dp)
                .height(24.dp)
                .background(color, shape = MaterialTheme.shapes.small)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = category.displayName.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold,
            color = color,
            letterSpacing = 1.2.sp
        )
    }
}

@Composable
private fun DrugCard(drug: Drug, onEdit: () -> Unit, onDelete: () -> Unit) {
    val categoryColor = drugCategoryColor(drug.category)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AstmColors.Surface),
        border = BorderStroke(1.dp, AstmColors.CardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Color stripe
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .fillMaxHeight()
                    .background(categoryColor)
            )
            Column(modifier = Modifier
                .weight(1f)
                .padding(12.dp)) {
                Text(
                    text = drug.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AstmColors.OnSurface
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    InfoChip("${drug.doseMgKg} mg/kg")
                    InfoChip("${drug.concentrationMgMl} mg/ml")
                    InfoChip("⏱ ${drug.reinjectionTimeMinutes} min")
                }
                if (drug.antidote.isNotBlank() && drug.antidote != "N/A") {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "💊 Antídoto: ${drug.antidote}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AstmColors.Safe
                    )
                }
            }
            Column(
                modifier = Modifier.padding(4.dp),
                verticalArrangement = Arrangement.Center
            ) {
                IconButton(onClick = onEdit, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar",
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar",
                        tint = Color(0xFFB71C1C), modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun InfoChip(text: String) {
    Surface(
        color = AstmColors.Background,
        border = BorderStroke(1.dp, AstmColors.Outline),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = AstmColors.OnSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DrugEditDialog(
    drug: Drug?,
    onDismiss: () -> Unit,
    onSave: (Drug) -> Unit
) {
    var name by remember { mutableStateOf(drug?.name ?: "") }
    var category by remember { mutableStateOf(drug?.category ?: DrugCategory.HIPNOTICO.name) }
    var doseMgKg by remember { mutableStateOf(drug?.doseMgKg?.toString() ?: "") }
    var concentrationMgMl by remember { mutableStateOf(drug?.concentrationMgMl?.toString() ?: "") }
    var reinjectionMinutes by remember { mutableStateOf(drug?.reinjectionTimeMinutes?.toString() ?: "") }
    var antidote by remember { mutableStateOf(drug?.antidote ?: "") }
    var notes by remember { mutableStateOf(drug?.notes ?: "") }
    var expandedCategory by remember { mutableStateOf(false) }
    var errors by remember { mutableStateOf<List<String>>(emptyList()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (drug == null) "Nuevo Fármaco" else "Editar Fármaco", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (errors.isNotEmpty()) {
                    errors.forEach { err ->
                        Text("• $err", color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Nombre del fármaco*") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                // Category dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedCategory,
                    onExpandedChange = { expandedCategory = it }
                ) {
                    OutlinedTextField(
                        value = DrugCategory.entries.firstOrNull { it.name == category }?.displayName ?: category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoría*") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedCategory) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedCategory,
                        onDismissRequest = { expandedCategory = false }
                    ) {
                        DrugCategory.entries.forEach { cat ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(Modifier.size(12.dp).background(drugCategoryColor(cat.name),
                                            shape = MaterialTheme.shapes.small))
                                        Spacer(Modifier.width(8.dp))
                                        Text(cat.displayName)
                                    }
                                },
                                onClick = { category = cat.name; expandedCategory = false }
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = doseMgKg, onValueChange = { doseMgKg = it },
                        label = { Text("Dosis mg/kg*") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = concentrationMgMl, onValueChange = { concentrationMgMl = it },
                        label = { Text("Conc. mg/ml*") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }
                OutlinedTextField(
                    value = reinjectionMinutes, onValueChange = { reinjectionMinutes = it },
                    label = { Text("Tiempo reinyección (min)*") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = antidote, onValueChange = { antidote = it },
                    label = { Text("Antídoto") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = notes, onValueChange = { notes = it },
                    label = { Text("Notas") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val errs = mutableListOf<String>()
                if (name.isBlank()) errs.add("Nombre requerido")
                val dose = doseMgKg.toDoubleOrNull()
                if (dose == null || dose <= 0) errs.add("Dosis inválida")
                val conc = concentrationMgMl.toDoubleOrNull()
                if (conc == null || conc <= 0) errs.add("Concentración inválida")
                val mins = reinjectionMinutes.toIntOrNull()
                if (mins == null || mins <= 0) errs.add("Tiempo de reinyección inválido")
                if (errs.isEmpty()) {
                    onSave(
                        Drug(
                            id = drug?.id ?: 0,
                            name = name.trim(),
                            category = category,
                            doseMgKg = dose!!,
                            concentrationMgMl = conc!!,
                            reinjectionTimeMinutes = mins!!,
                            antidote = antidote.trim(),
                            notes = notes.trim()
                        )
                    )
                } else {
                    errors = errs
                }
            }) { Text("Guardar") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
