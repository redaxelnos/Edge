package com.example.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Investigation
import com.example.data.model.InvestigationStatus
import com.example.ui.theme.GridAmber
import com.example.ui.theme.GridCyan
import com.example.ui.theme.GridEmerald
import com.example.ui.theme.GridGreen
import com.example.ui.theme.GridNavy
import com.example.ui.theme.GridOrange
import java.util.Locale

@Composable
fun InvestigationListSection(
    investigations: List<Investigation>,
    selectedInvestigation: Investigation?,
    onSelect: (Investigation) -> Unit,
    onCreate: (
        title: String,
        tech: String,
        vendor: String,
        desc: String,
        capex: Double,
        opex: Double,
        saidi: Double,
        payback: Double,
        roi: Double,
        feasibility: Double,
        feeder: String,
        weeks: Int
    ) -> Unit,
    onEdit: (Investigation) -> Unit,
    onDelete: (String) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf<InvestigationStatus?>(null) }

    val filteredInvestigations = remember(investigations, searchQuery, selectedStatusFilter) {
        investigations.filter { inv ->
            val matchesQuery = searchQuery.isBlank() ||
                inv.title.contains(searchQuery, ignoreCase = true) ||
                inv.technologyType.contains(searchQuery, ignoreCase = true) ||
                inv.vendor.contains(searchQuery, ignoreCase = true) ||
                inv.description.contains(searchQuery, ignoreCase = true)
            val matchesStatus = selectedStatusFilter == null || inv.status == selectedStatusFilter?.name
            matchesQuery && matchesStatus
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New Investigation") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("create_investigation_fab")
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
                HeroBannerCard(
                    investigationCount = investigations.size,
                    activePilotsCount = investigations.count { it.isPilotStageOrBeyond() },
                    avgSaidiGain = if (investigations.isNotEmpty()) investigations.map { it.saidiImprovement }.average() else 0.0,
                    avgFeasibility = if (investigations.isNotEmpty()) investigations.map { it.pilotFeasibilityScore }.average() else 0.0
                )
            }

            if (investigations.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search investigations by title, technology, vendor, or scope...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_investigations_input")
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedStatusFilter == null,
                                onClick = { selectedStatusFilter = null },
                                label = { Text("All Stages (${investigations.size})") }
                            )
                            InvestigationStatus.entries.forEach { st ->
                                val count = investigations.count { it.status == st.name }
                                FilterChip(
                                    selected = selectedStatusFilter == st,
                                    onClick = {
                                        selectedStatusFilter = if (selectedStatusFilter == st) null else st
                                    },
                                    label = { Text("${st.label} ($count)") }
                                )
                            }
                        }
                    }
                }
            }

            if (investigations.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.ElectricBolt,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = GridOrange
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No Technology Investigations Found",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Create a custom investigation with your own financial and reliability parameters to begin.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { showCreateDialog = true },
                                modifier = Modifier.testTag("empty_create_investigation_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Create Custom Investigation")
                            }
                        }
                    }
                }
            } else {
                items(filteredInvestigations, key = { it.id }) { inv ->
                    InvestigationCard(
                        investigation = inv,
                        isSelected = inv.id == selectedInvestigation?.id,
                        onSelect = { onSelect(inv) },
                        onEdit = { onEdit(inv) },
                        onDelete = { onDelete(inv.id) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }

    if (showCreateDialog) {
        CreateInvestigationDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { title, tech, vendor, desc, capex, opex, saidi, payback, roi, feasibility, feeder, weeks ->
                onCreate(title, tech, vendor, desc, capex, opex, saidi, payback, roi, feasibility, feeder, weeks)
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun HeroBannerCard(
    investigationCount: Int,
    activePilotsCount: Int,
    avgSaidiGain: Double,
    avgFeasibility: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F2942),
                            GridNavy
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "UTILITY PORTFOLIO COMMAND",
                        style = MaterialTheme.typography.labelMedium,
                        color = GridCyan,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = GridEmerald.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "$activePilotsCount Active Field Pilot(s)",
                            style = MaterialTheme.typography.labelSmall,
                            color = GridEmerald,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                Text(
                    text = "Workforce Augmentation, Safety Guardrails & Process Integration",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HeroMetricBadge(
                        label = "Investigations",
                        value = "$investigationCount Programs",
                        modifier = Modifier.weight(1f)
                    )
                    HeroMetricBadge(
                        label = "Avg SAIDI Reliability",
                        value = String.format(Locale.US, "+%.1f%% Gain", avgSaidiGain),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HeroMetricBadge(
                        label = "Workflow Readiness",
                        value = String.format(Locale.US, "%.0f/100 Score", avgFeasibility),
                        modifier = Modifier.weight(1f)
                    )
                    HeroMetricBadge(
                        label = "Adoption Focus",
                        value = "Safety & Simplicity",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroMetricBadge(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.12f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.78f)
            )
        }
    }
}

@Composable
fun InvestigationCard(
    investigation: Investigation,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var expandedDescription by remember { mutableStateOf(false) }

    val statusEnum = runCatching { InvestigationStatus.valueOf(investigation.status) }
        .getOrDefault(InvestigationStatus.INVESTIGATION)
    val statusColor = when (statusEnum) {
        InvestigationStatus.INVESTIGATION -> GridCyan
        InvestigationStatus.BUSINESS_CASE -> GridOrange
        InvestigationStatus.PILOT -> GridGreen
        InvestigationStatus.CHANGE_MANAGEMENT -> GridAmber
        InvestigationStatus.ECOSYSTEM_ADOPTION -> Color(0xFF8B5CF6)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { onSelect() }
            .testTag("investigation_card_${investigation.id}"),
        shape = RoundedCornerShape(16.dp),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = statusColor.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = statusEnum.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColor,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = investigation.technologyType,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        if (isSelected) {
                            Surface(
                                color = GridGreen.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "ACTIVE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GridGreen,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = investigation.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Primary Vendor: ${investigation.vendor} • Target: ${investigation.pilotFeederSubstation}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.testTag("edit_investigation_${investigation.id}")
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Investigation Attributes",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("delete_investigation_${investigation.id}")
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Delete Investigation",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            if (investigation.description.isNotBlank()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedDescription = !expandedDescription }
                ) {
                    Text(
                        text = investigation.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (expandedDescription) Int.MAX_VALUE else 3
                    )
                    if (investigation.description.length > 140) {
                        Text(
                            text = if (expandedDescription) "Show less" else "Read full technical scope...",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            HorizontalDivider()

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricPill(
                        label = "Labor & Safety Gain",
                        value = String.format(Locale.US, "+%.0f%% Efficiency • Zero Job Cuts", (investigation.projectedRoi * 0.65).coerceIn(14.0, 48.0)),
                        modifier = Modifier.weight(1f)
                    )
                    MetricPill(
                        label = "Target Feeder & Window",
                        value = "${investigation.pilotFeederSubstation.take(18)} (${investigation.pilotDurationWeeks}w)",
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricPill(
                        label = "SAIDI & Injury Guardrail",
                        value = String.format(Locale.US, "+%.1f%% Reliability & Safety", investigation.saidiImprovement),
                        modifier = Modifier.weight(1f)
                    )
                    MetricPill(
                        label = "Simplicity & Buy-In",
                        value = String.format(Locale.US, "%.0f/100 Fit • %.0f%% ADKAR", investigation.pilotFeasibilityScore, investigation.changeManagementScore),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateInvestigationDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        tech: String,
        vendor: String,
        desc: String,
        capex: Double,
        opex: Double,
        saidi: Double,
        payback: Double,
        roi: Double,
        feasibility: Double,
        feeder: String,
        weeks: Int
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var techType by remember { mutableStateOf("ADMS") }
    var vendor by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var capex by remember { mutableStateOf("3.5") }
    var opex by remember { mutableStateOf("0.45") }
    var saidi by remember { mutableStateOf("14.0") }
    var payback by remember { mutableStateOf("3.2") }
    var roi by remember { mutableStateOf("155.0") }
    var feasibility by remember { mutableStateOf("84.0") }
    var feeder by remember { mutableStateOf("Substation 14 - Feeder 3") }
    var weeks by remember { mutableStateOf("12") }

    val techOptions = listOf("ADMS", "DERMS", "AMI 2.0", "FLISR", "DLR", "Substation Automation", "BESS Control")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 700.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "New Grid Technology Investigation",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Investigation Title *") },
                        placeholder = { Text("e.g., Next-Gen DERMS Dispatch & Volt/VAR") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_inv_title"),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = techType,
                            onValueChange = { techType = it },
                            label = { Text("Technology Domain *") },
                            placeholder = { Text("ADMS, DERMS, or custom") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_inv_tech"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = vendor,
                            onValueChange = { vendor = it },
                            label = { Text("Primary Vendor *") },
                            placeholder = { Text("e.g., GE Vernova / Siemens") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_inv_vendor"),
                            singleLine = true
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        techOptions.forEach { option ->
                            FilterChip(
                                selected = techType.equals(option, ignoreCase = true),
                                onClick = { techType = option },
                                label = { Text(option, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Technical Scope & Grid Objectives") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_inv_desc"),
                        minLines = 3
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        Text(
                            text = "Note: Premature CAPEX/OPEX estimates are deferred until vendor contract negotiations and scope agreements conclude. Focus on operational efficiency, safety guardrails, and proven workflow fit.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(10.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = roi,
                            onValueChange = { roi = it },
                            label = { Text("Target Efficiency Gain (%)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = payback,
                            onValueChange = { payback = it },
                            label = { Text("Rollout Horizon (Yrs)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = saidi,
                            onValueChange = { saidi = it },
                            label = { Text("Target SAIDI Gain (%)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_inv_saidi"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = feasibility,
                            onValueChange = { feasibility = it },
                            label = { Text("Feasibility (0-100)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = feeder,
                            onValueChange = { feeder = it },
                            label = { Text("Target Pilot Feeder") },
                            modifier = Modifier.weight(1.4f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = weeks,
                            onValueChange = { weeks = it },
                            label = { Text("Pilot Weeks") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(0.8f),
                            singleLine = true
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank() && vendor.isNotBlank()) {
                                onConfirm(
                                    title.trim(),
                                    techType.trim().ifBlank { "Grid Modernization" },
                                    vendor.trim(),
                                    description.trim(),
                                    capex.toDoubleOrNull() ?: 3.0,
                                    opex.toDoubleOrNull() ?: 0.4,
                                    saidi.toDoubleOrNull() ?: 12.0,
                                    payback.toDoubleOrNull() ?: 3.2,
                                    roi.toDoubleOrNull() ?: 155.0,
                                    (feasibility.toDoubleOrNull() ?: 84.0).coerceIn(0.0, 100.0),
                                    feeder.trim().ifBlank { "Substation 14 - Feeder 3" },
                                    (weeks.toIntOrNull() ?: 12).coerceAtLeast(1)
                                )
                            }
                        },
                        enabled = title.isNotBlank() && vendor.isNotBlank(),
                        modifier = Modifier.testTag("confirm_create_investigation")
                    ) {
                        Text("Create Investigation")
                    }
                }
            }
        }
    }
}
