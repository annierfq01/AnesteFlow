package com.anestesia.app.presentation.about

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anestesia.app.presentation.theme.AstmColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onNavigateBack: () -> Unit) {

    // Animación del pulso del ícono médico
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ring_alpha"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Acerca de", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AstmColors.Surface,
                    titleContentColor = AstmColors.OnSurface
                )
            )
        },
        containerColor = AstmColors.Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            Spacer(Modifier.height(8.dp))

            // ── App icon con pulso ────────────────────────────────────────────
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(120.dp)
            ) {
                // Anillo exterior pulsante
                Box(
                    modifier = Modifier
                        .size((120 * pulseScale).dp)
                        .drawBehind {
                            drawCircle(
                                color = Color(0xFF0D47A1).copy(alpha = ringAlpha),
                                radius = size.minDimension / 2,
                                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                )
                // Círculo sólido con cruz médica
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0xFF1565C0), Color(0xFF0D47A1))
                            )
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.MedicalServices,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(52.dp)
                    )
                }
            }

            // ── App name & version ────────────────────────────────────────────
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "AnestesIA",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AstmColors.OnSurface,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "Versión 1.0.0",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AstmColors.OnSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Surface(
                    color = Color(0xFF0D47A1).copy(alpha = 0.08f),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color(0xFF0D47A1).copy(alpha = 0.2f))
                ) {
                    Text(
                        text = "Software de Apoyo a la Decisión Clínica",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF0D47A1),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // ── Developer card ────────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AstmColors.Surface),
                border = BorderStroke(1.dp, AstmColors.CardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Avatar con iniciales
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF1565C0), Color(0xFF0D47A1))
                                )
                            )
                    ) {
                        Text(
                            text = "AJ",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Annier Jesús Fajardo Quesada",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = AstmColors.OnSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Desarrollador de Software",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AstmColors.OnSurfaceVariant,
                            fontStyle = FontStyle.Italic
                        )
                    }

                    HorizontalDivider(color = AstmColors.CardBorder)

                    // Institución
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Business,
                            contentDescription = null,
                            tint = Color(0xFF0D47A1),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Unión de Informáticos de Cuba",
                                fontWeight = FontWeight.SemiBold,
                                color = AstmColors.OnSurface,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "UIC",
                                style = MaterialTheme.typography.labelSmall,
                                color = AstmColors.OnSurfaceVariant,
                                letterSpacing = 1.5.sp
                            )
                        }
                    }
                }
            }

            // ── App info grid ─────────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AstmColors.Surface),
                border = BorderStroke(1.dp, AstmColors.CardBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Información técnica",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = AstmColors.OnSurfaceVariant,
                        letterSpacing = 0.8.sp
                    )
                    InfoRow(Icons.Default.Code,          "Plataforma",    "Android (minSdk 26+)")
                    InfoRow(Icons.Default.Architecture,  "Arquitectura",  "MVVM + Clean Architecture")
                    InfoRow(Icons.Default.Storage,       "Base de datos", "Room (SQLite)")
                    InfoRow(Icons.Default.Palette,       "Colores",       "Código ASTM Anestesiología")
                    InfoRow(Icons.Default.Notifications, "Alertas",       "Foreground Service persistente")
                }
            }

            // ── Disclaimer médico ─────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFF8E1)
                ),
                border = BorderStroke(1.dp, Color(0xFFFFCA28)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFFF9A825),
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "Este software es una herramienta de apoyo a la decisión clínica. " +
                               "No reemplaza el criterio del profesional médico. " +
                               "Su uso queda bajo responsabilidad exclusiva del anestesiólogo a cargo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF5D4037),
                        lineHeight = 18.sp
                    )
                }
            }

            // ── Copyright ─────────────────────────────────────────────────────
            Text(
                text = "© 2025 Annier Jesús Fajardo Quesada · UIC",
                style = MaterialTheme.typography.labelSmall,
                color = AstmColors.Outline,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(icon, contentDescription = null,
            tint = Color(0xFF0D47A1), modifier = Modifier.size(18.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = AstmColors.OnSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold, color = AstmColors.OnSurface)
        }
    }
}
