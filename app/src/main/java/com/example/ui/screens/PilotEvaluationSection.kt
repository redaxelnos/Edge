package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Investigation
import com.example.data.model.InvestigationStatus
import com.example.data.model.KpiCategory
import com.example.data.model.PilotKpi
import com.example.ui.theme.GridAmber
import com.example.ui.theme.GridCyan
import com.example.ui.theme.GridEmerald
import com.example.ui.theme.GridGreen
import com.example.ui.theme.GridNavy
import com.example.ui.theme.GridOrange
import java.util.Locale

@Composable
fun PilotEvaluationSection(
    investigation: Investigation?,
    pilotKpis: List<PilotKpi>,
    onPushToPilot: (String, Int) -> Unit,
    onAddKpi: (metricName: String, category: String, currentValue: Double, targetValue: Double, unit: String, notes: String) -> Unit,
    onUpdateKpi: (PilotKpi) -> Unit = {},
    onDeleteKpi: (String) -> Unit,
    onUpdateInvestigation: (Investigation) -> Unit = {}
) {
    if (investigation == null) {
        EmptySelectionPrompt("Select an investigation in the Projects tab to manage field pilot deployment and KPIs.")
        return
    }

    val isPilotActive = investigation.isPilotStageOrBeyond()

    var feederInput by remember(investigation.id, investigation.pilotFeederSubstation) {
        mutableStateOf(investigation.pilotFeederSubstation.ifBlank { "Substation 14 - Feeder 3" })
    }
    var weeksInput by remember(investigation.id, investigation.pilotDurationWeeks) {
        mutableStateOf(investigation.pilotDurationWeeks.toString())
    }
    var showKpiDialog by remember { mutableStateOf(false) }
    var editingKpi by remember { mutableStateOf<PilotKpi?>(null) }
    var isEditingPilotConfig by remember { mutableStateOf(false) }

    // Strictly factual telemetry calculations from actual logged KPIs
    val metCount = remember(pilotKpis) {
        pilotKpis.count { kpi -> isKpiTargetMet(kpi) }
    }
    val targetAttainmentText = remember(pilotKpis, metCount) {
        if (pilotKpis.isEmpty()) {
            "No KPIs Logged"
        } else {
            val pct = (metCount * 100) / pilotKpis.size
            "$metCount/${pilotKpis.size} Met ($pct%)"
        }
    }
    val measuredProgressPct = remember(pilotKpis) {
        if (pilotKpis.isEmpty()) {
            null
        } else {
            pilotKpis.map { computeKpiProgressRatio(it) * 100.0 }.average()
        }
    }
    val uptimeKpis = remember(pilotKpis) {
        pilotKpis.filter {
            it.category.equals(KpiCategory.UPTIME.name, ignoreCase = true) ||
                it.metricName.contains("Uptime", ignoreCase = true) ||
                it.metricName.contains("Availability", ignoreCase = true)
        }
    }
    val uptimeDisplayText = remember(uptimeKpis) {
        if (uptimeKpis.isNotEmpty()) {
            String.format(Locale.US, "%.2f%%", uptimeKpis.map { it.currentValue }.average())
        } else {
            "Not Logged"
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editingKpi = null
                    showKpiDialog = true
                },
                icon = { Icon(Icons.Default.AddChart, contentDescription = null) },
                text = { Text("Log Field KPI") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_kpi_fab")
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isPilotActive) GridNavy else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (isPilotActive) Icons.Default.Radar else Icons.Default.RocketLaunch,
                                    contentDescription = null,
                                    tint = if (isPilotActive) GridGreen else GridOrange
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isPilotActive) "FIELD PILOT ACTIVE • TELEMETRY LIVE" else "READY FOR FIELD PILOT DEPLOYMENT",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isPilotActive) GridGreen else GridOrange,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            if (isPilotActive) {
                                IconButton(
                                    onClick = { isEditingPilotConfig = !isEditingPilotConfig },
                                    modifier = Modifier.testTag("edit_active_pilot_config_btn")
                                ) {
                                    Icon(
                                        if (isEditingPilotConfig) Icons.Default.Close else Icons.Default.Edit,
                                        contentDescription = "Edit Pilot Feeder & Duration",
                                        tint = Color.White
                                    )
                                }
                            }
                        }

                        if (!isPilotActive || isEditingPilotConfig) {
                            Text(
                                text = if (isPilotActive) {
                                    "Update target substation feeder, duration, or pilot activation status."
                                } else {
                                    "Transition ${investigation.title} from investigation into a live utility substation pilot to benchmark field performance."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isPilotActive) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedTextField(
                                value = feederInput,
                                onValueChange = { feederInput = it },
                                label = { Text("Target Substation / Feeder") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_pilot_feeder"),
                                singleLine = true,
                                colors = if (isPilotActive) {
                                    OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedLabelColor = GridCyan,
                                        unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                                        focusedBorderColor = GridCyan,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.4f)
                                    )
                                } else {
                                    OutlinedTextFieldDefaults.colors()
                                }
                            )
                            OutlinedTextField(
                                value = weeksInput,
                                onValueChange = { weeksInput = it },
                                label = { Text("Pilot Evaluation Duration (Weeks)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_pilot_weeks"),
                                singleLine = true,
                                colors = if (isPilotActive) {
                                    OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedLabelColor = GridCyan,
                                        unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                                        focusedBorderColor = GridCyan,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.4f)
                                    )
                                } else {
                                    OutlinedTextFieldDefaults.colors()
                                }
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        onPushToPilot(feederInput.trim(), weeksInput.toIntOrNull() ?: 12)
                                        isEditingPilotConfig = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = GridGreen),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("push_to_pilot_button")
                                ) {
                                    Icon(Icons.Default.Bolt, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        if (isPilotActive) "Save Pilot Parameters" else "Push to Field Pilot Now",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                if (isPilotActive) {
                                    OutlinedButton(
                                        onClick = {
                                            onUpdateInvestigation(
                                                investigation.copy(status = InvestigationStatus.BUSINESS_CASE.name)
                                            )
                                            isEditingPilotConfig = false
                                        },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GridOrange),
                                        border = BorderStroke(1.dp, GridOrange)
                                    ) {
                                        Text("Return to Pre-Pilot")
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "Deployed at: ${investigation.pilotFeederSubstation}",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = String.format(
                                    Locale.US,
                                    "Evaluation Window: %d Weeks • Measured Progress: %s",
                                    investigation.pilotDurationWeeks,
                                    measuredProgressPct?.let { String.format(Locale.US, "%.1f%%", it) } ?: "Awaiting KPIs"
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )

                            // 2x2 Responsive Live Telemetry Summary Grid (Zero Fabricated Numbers)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                PilotSummaryMetricBox(
                                    label = "KPIs Logged",
                                    value = "${pilotKpis.size} Metrics",
                                    accentColor = GridCyan,
                                    modifier = Modifier.weight(1f)
                                )
                                PilotSummaryMetricBox(
                                    label = "Target Attainment",
                                    value = targetAttainmentText,
                                    accentColor = if (metCount > 0) GridGreen else GridAmber,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                PilotSummaryMetricBox(
                                    label = "Measured Progress",
                                    value = measuredProgressPct?.let { String.format(Locale.US, "%.1f%%", it) } ?: "Awaiting Data",
                                    accentColor = GridEmerald,
                                    modifier = Modifier.weight(1f)
                                )
                                PilotSummaryMetricBox(
                                    label = "Measured Uptime",
                                    value = uptimeDisplayText,
                                    accentColor = Color.White,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            if (pilotKpis.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Pilot Telemetry vs. Target Benchmark",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Green bars show measured field telemetry; Cyan bars show target pilot benchmark",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            val chartPoints = pilotKpis.take(5).map { kpi ->
                                NativeBarPoint(
                                    label = kpi.metricName.take(12),
                                    value = kpi.currentValue.toFloat(),
                                    secondaryValue = kpi.targetValue.toFloat()
                                )
                            }
                            NativeFinancialBarChart(
                                points = chartPoints,
                                primaryColor = GridGreen,
                                secondaryColor = GridCyan,
                                unitSuffix = ""
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Field Pilot KPIs & Telemetry (${pilotKpis.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (pilotKpis.isNotEmpty() && measuredProgressPct != null) {
                        TextButton(
                            onClick = {
                                onUpdateInvestigation(
                                    investigation.copy(
                                        pilotSuccessRate = Math.round(measuredProgressPct * 10.0) / 10.0
                                    )
                                )
                            }
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                String.format(Locale.US, "Sync %.1f%% to Project", measuredProgressPct),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }

            if (pilotKpis.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Speed,
                                contentDescription = null,
                                modifier = Modifier.size(44.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No Pilot KPIs Logged Yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Tap 'Log Field KPI' to record measured current telemetry and target benchmarks (e.g., Fault Isolation Time, Voltage Deviation, SCADA Latency, System Uptime).",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(pilotKpis, key = { it.id }) { kpi ->
                    KpiTelemetryCard(
                        kpi = kpi,
                        onEdit = {
                            editingKpi = kpi
                            showKpiDialog = true
                        },
                        onDelete = { onDeleteKpi(kpi.id) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }

    if (showKpiDialog) {
        KpiEditorDialog(
            initialKpi = editingKpi,
            onDismiss = {
                showKpiDialog = false
                editingKpi = null
            },
            onSaveNew = { name, cat, curr, target, unit, notes ->
                onAddKpi(name, cat, curr, target, unit, notes)
                showKpiDialog = false
            },
            onSaveEdited = { updated ->
                onUpdateKpi(updated)
                showKpiDialog = false
                editingKpi = null
            }
        )
    }
}

private fun isKpiTargetMet(kpi: PilotKpi): Boolean {
    return if (kpi.targetValue > 0.0) {
        kpi.currentValue >= kpi.targetValue
    } else {
        kpi.currentValue <= kpi.targetValue
    }
}

private fun computeKpiProgressRatio(kpi: PilotKpi): Float {
    return if (kpi.targetValue > 0.0001) {
        (kpi.currentValue / kpi.targetValue).coerceIn(0.0, 1.0).toFloat()
    } else {
        1f
    }
}

@Composable
private fun PilotSummaryMetricBox(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.1f),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.72f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = accentColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun KpiTelemetryCard(
    kpi: PilotKpi,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val progress = computeKpiProgressRatio(kpi)
    val metTarget = isKpiTargetMet(kpi)
    val catLabel = runCatching { KpiCategory.valueOf(kpi.category).label }.getOrDefault(kpi.category)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("kpi_card_${kpi.id}"),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = catLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Surface(
                            color = (if (metTarget) GridGreen else GridAmber).copy(alpha = 0.16f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (metTarget) "TARGET MET" else "${(progress * 100).toInt()}% OF TARGET",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (metTarget) GridGreen else GridAmber,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = kpi.metricName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.testTag("edit_kpi_${kpi.id}")
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit KPI",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("delete_kpi_${kpi.id}")
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Delete KPI",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KpiNumberColumn(
                    label = "Measured Telemetry",
                    value = "${kpi.currentValue} ${kpi.unit}",
                    color = GridGreen,
                    modifier = Modifier.weight(1f)
                )
                KpiNumberColumn(
                    label = "Target Benchmark",
                    value = "${kpi.targetValue} ${kpi.unit}",
                    color = GridCyan,
                    modifier = Modifier.weight(1f)
                )
            }

            if (kpi.notes.isNotBlank()) {
                Text(
                    text = kpi.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = if (metTarget) GridGreen else GridAmber,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
fun KpiNumberColumn(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun KpiEditorDialog(
    initialKpi: PilotKpi?,
    onDismiss: () -> Unit,
    onSaveNew: (metricName: String, category: String, currentValue: Double, targetValue: Double, unit: String, notes: String) -> Unit,
    onSaveEdited: (PilotKpi) -> Unit
) {
    var name by remember { mutableStateOf(initialKpi?.metricName ?: "") }
    var category by remember { mutableStateOf(initialKpi?.category ?: KpiCategory.RELIABILITY.name) }
    var current by remember { mutableStateOf((initialKpi?.currentValue ?: 18.0).toString()) }
    var target by remember { mutableStateOf((initialKpi?.targetValue ?: 20.0).toString()) }
    var unit by remember { mutableStateOf(initialKpi?.unit ?: "%") }
    var notes by remember { mutableStateOf(initialKpi?.notes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialKpi == null) "Log Field Pilot KPI" else "Edit Field Pilot KPI",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Metric Name *") },
                    placeholder = { Text("e.g., FLISR Fault Isolation Speed Gain") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_kpi_name"),
                    singleLine = true
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    KpiCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = category == cat.name,
                            onClick = {
                                category = cat.name
                                if (initialKpi == null) unit = cat.defaultUnit
                            },
                            label = { Text(cat.label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = current,
                        onValueChange = { current = it },
                        label = { Text("Current Value") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = target,
                        onValueChange = { target = it },
                        label = { Text("Target Goal") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text("Unit (e.g., %, ms, min, V)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Telemetry Notes / Substation Conditions") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val c = current.toDoubleOrNull() ?: 0.0
                        val t = target.toDoubleOrNull() ?: 0.0
                        val u = unit.trim().ifBlank { "%" }
                        if (initialKpi == null) {
                            onSaveNew(name.trim(), category, c, t, u, notes.trim())
                        } else {
                            onSaveEdited(
                                initialKpi.copy(
                                    metricName = name.trim(),
                                    category = category,
                                    currentValue = c,
                                    targetValue = t,
                                    unit = u,
                                    notes = notes.trim()
                                )
                            )
                        }
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("confirm_add_kpi")
            ) {
                Text(if (initialKpi == null) "Save KPI" else "Update KPI")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
